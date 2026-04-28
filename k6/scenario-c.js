import http from 'k6/http';
import {check} from 'k6';
import {Trend} from 'k6/metrics';
import {authHeaders, loginUsers} from './common.js';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const MAX_VUS = parseInt(__ENV.MAX_VUS || '200');

// before / after 구분 태그 — InfluxDB에서 두 결과를 같은 패널에 겹쳐서 비교할 때 사용
// 실행 시 -e RUN_TAG=before 또는 -e RUN_TAG=after 로 전달
const RUN_TAG = __ENV.RUN_TAG || 'default';

// POST /payments 레이턴시만 별도로 추적하는 커스텀 메트릭
// Grafana 쿼리: SELECT percentile("value", 95) FROM "payment_duration_ms" WHERE "run" = 'before'/'after'
const paymentDuration = new Trend('payment_duration_ms', true);

// load: before/after 비교용 안정 구간 확보
// smoke: 시나리오 동작 검증용 최소 실행
const stages = {
    smoke: [
        {target: 3, duration: '30s'},
    ],
    load: [
        {target: 50, duration: '1m'},  // 워밍업
        {target: 100, duration: '3m'},  // 안정 구간 — 이 구간 지표로 before/after 비교
        {target: 0, duration: '30s'},  // 쿨다운
    ],
};

const testType = stages[__ENV.TEST_TYPE] ? __ENV.TEST_TYPE : 'smoke';
if (__ENV.TEST_TYPE && !stages[__ENV.TEST_TYPE]) {
    console.warn(`TEST_TYPE="${__ENV.TEST_TYPE}" 은 유효하지 않습니다. smoke 로 실행합니다. (유효값: smoke, load)`);
}

export const options = {
    scenarios: {
        payment_confirm_flow: {
            executor: 'ramping-arrival-rate',
            startRate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 100,
            maxVUs: MAX_VUS,
            stages: stages[testType],
        },
    },
    // 모든 메트릭에 run 태그 자동 부착 → InfluxDB에서 before/after 필터링 가능
    tags: {run: RUN_TAG},
    thresholds: {
        // 조회 기준
        'http_req_duration{name:product_list}': [
            'p(95)<500'
        ],

        // 쓰기 기준
        'http_req_duration{name:cart_add}': [
            'p(95)<800'
        ],

        'http_req_duration{name:order_create}': [
            'p(95)<1000'
        ],

        // 핵심: 결제
        'http_req_duration{name:payment_create}': [
            'p(95)<2000'
        ],

        'payment_duration_ms': [
            'p(95)<2000'
        ],

        'http_req_failed{name:payment_create}': [
            'rate<0.01'
        ],

        'http_req_failed{name:order_create}': [
            'rate<0.01'
        ],
    },
};

// POST 요청용: Authorization + Content-Type + 엔드포인트 name 태그
function postParams(token, endpointName) {
    return {
        headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
        tags: {name: endpointName},
    };
}

export function setup() {
    const productRes = http.get(`${BASE_URL}/products?page=0&size=20`,
        {
            tags: {name: 'product_list'},
        }
    );

    if (productRes.status !== 200) {
        throw new Error(`Product fetch failed: status=${productRes.status}`);
    }

    const products = productRes.json('data.content');

    if (!Array.isArray(products) || products.length === 0) {
        throw new Error('No products found. Seed products before running test.');
    }

    const productIds = products.map(p => p.id);

    const {tokens} = loginUsers(MAX_VUS);

    const users = tokens.map(token => {
        const addrRes = http.get(`${BASE_URL}/addresses`, authHeaders(token));
        const addresses = addrRes.json('data');
        const addressId = addresses && addresses.length > 0 ? addresses[0].addressId : null;

        if (!addressId) {
            throw new Error('Address missing for token. Seed addresses first.');
        }

        return {token, addressId};
    });

    if (users.length === 0) {
        throw new Error('No users with valid addressId. Seed addresses before running this scenario.');
    }

    return {users, productIds};
}

export default function (data) {
    const user = data.users[(__VU - 1) % data.users.length];
    const productId = data.productIds[Math.floor(Math.random() * data.productIds.length)];

    // 1. 장바구니 추가
    const cartRes = http.post(
        `${BASE_URL}/carts/items?sort=createdAt,desc&size=1`,
        JSON.stringify({productId, quantity: 1}),
        postParams(user.token, 'cart_add')
    );

    check(cartRes, {'cart item added 201': r => r.status === 201});
    if (cartRes.status !== 201) {
        const bodyPreview = (cartRes.body || '').slice(0, 300);
        console.error(`CART FAILED: status=${cartRes.status}, bodyPreview=${bodyPreview}`);
        return;
    }

    const cartItemId = cartRes.json().data.items.content[0].id;

    // 2. 주문 생성
    const orderRes = http.post(
        `${BASE_URL}/orders`,
        JSON.stringify({cartItemIds: [cartItemId], addressId: user.addressId}),
        postParams(user.token, 'order_create')
    );

    check(orderRes, {'order created 201': r => r.status === 201});
    if (orderRes.status !== 201) {
        const bodyPreview = (orderRes.body || '').slice(0, 300);
        console.error(`ORDER FAILED: status=${orderRes.status}, bodyPreview=${bodyPreview}`);
        return;
    }

    const orderId = orderRes.json('data.orderId');

    // 3. 결제 확인 (createPayment + confirmPayment + updateSellerDashboard 포함)
    //    이 요청의 레이턴시가 @Async 전후 비교 대상
    const paymentStart = Date.now();
    const paymentRes = http.post(
        `${BASE_URL}/payments`,
        JSON.stringify({orderId, method: 'MOCK'}),
        postParams(user.token, 'payment_create')
    );
    paymentDuration.add(Date.now() - paymentStart, {run: RUN_TAG});

    check(paymentRes, {'payment processed 201': r => r.status === 201});
}
