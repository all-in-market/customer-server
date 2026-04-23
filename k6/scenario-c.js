import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { loginUsers, authHeaders } from './common.js';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const MAX_VUS  = parseInt(__ENV.MAX_VUS  || '200');

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
        { target: 3,  duration: '30s' },
    ],
    load: [
        { target: 50, duration: '1m'  },  // 워밍업
        { target: 100, duration: '3m'  },  // 안정 구간 — 이 구간 지표로 before/after 비교
        { target: 0,  duration: '30s' },  // 쿨다운
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
    tags: { run: RUN_TAG },
    thresholds: {
        // POST /payments 에 name 태그를 붙여 이 엔드포인트만 기준 적용
        'http_req_duration{name:POST /payments}': ['p(95)<2000'],
        'payment_duration_ms':                    ['p(95)<2000'],
        http_req_failed:                          ['rate<0.01'],
    },
};

// POST 요청용: Authorization + Content-Type + 엔드포인트 name 태그
function postParams(token, endpointName) {
    return {
        headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
        tags: { name: endpointName },
    };
}

export function setup() {
    const productRes = http.get(`${BASE_URL}/products?page=0&size=20`);
    const productIds = productRes.json('data.content').map(p => p.id);

    const { tokens } = loginUsers(MAX_VUS);

    const users = tokens.map(token => {
        const addrRes  = http.get(`${BASE_URL}/addresses`, authHeaders(token));
        const addrBody = addrRes.json();
        const addresses  = addrRes.json('data');
        const addressId  = addresses && addresses.length > 0 ? addresses[0].addressId : null;

        if (!addressId) {
            console.warn(`addressId missing, skipping user. status=${addrRes.status}`);
            return null;
        }

        return { token, addressId };
        // [수정] null 유저 제거 → default 함수에서 addressId 없는 유저가 섞이지 않음
    }).filter(user => user !== null);

    return { users, productIds };
}

export default function (data) {
    const user      = data.users[(__VU - 1) % data.users.length];
    const productId = data.productIds[Math.floor(Math.random() * data.productIds.length)];

    // 1. 장바구니 추가
    const cartRes = http.post(
        `${BASE_URL}/carts/items?sort=createdAt,desc&size=1`,
        JSON.stringify({ productId, quantity: 1 }),
        postParams(user.token, 'POST /carts/items')
    );

    check(cartRes, { 'cart item added 201': r => r.status === 201 });
    if (cartRes.status !== 201) return;

    const cartItemId = cartRes.json().data.items.content[0].id;

    // 2. 주문 생성
    const orderRes = http.post(
        `${BASE_URL}/orders`,
        JSON.stringify({ cartItemIds: [cartItemId], addressId: user.addressId }),
        postParams(user.token, 'POST /orders')
    );

    check(orderRes, { 'order created 201': r => r.status === 201 });
    if (orderRes.status !== 201) return;

    const orderId = orderRes.json('data.orderId');

    // 3. 결제 확인 (createPayment + confirmPayment + updateSellerDashboard 포함)
    //    이 요청의 레이턴시가 @Async 전후 비교 대상
    const paymentStart = Date.now();
    const paymentRes = http.post(
        `${BASE_URL}/payments`,
        JSON.stringify({ orderId, method: 'MOCK' }),
        postParams(user.token, 'POST /payments')
    );
    paymentDuration.add(Date.now() - paymentStart, { run: RUN_TAG });

    check(paymentRes, { 'payment processed 201': r => r.status === 201 });
}
