import http from 'k6/http';
import {check} from 'k6';
import {authHeaders, loginUsers} from './common.js';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const MAX_VUS = parseInt(__ENV.MAX_VUS || '500');

const stages = {
    smoke: [
        {target: 5, duration: '30s'},
    ],
    load: [
        {target: 10, duration: '1m'},
        {target: 20, duration: '1m'},
        {target: 30, duration: '1m'},
        {target: 0, duration: '30s'},
    ],

    pressure: [
        { target: 30, duration: '1m' },
        { target: 50, duration: '1m' },
        { target: 70, duration: '1m' },
        { target: 100, duration: '1m' },
        { target: 0, duration: '30s' },
    ],

    stress: [
        {target: 15, duration: '1m'},
        {target: 60, duration: '1m'},
        {target: 150, duration: '1m'},
        {target: 300, duration: '1m'},
        {target: 600, duration: '1m'},
        {target: 0, duration: '30s'},
    ],
};

export const options = {
    setupTimeout: '3m',
    scenarios: {
        purchase_flow: {
            executor: 'ramping-arrival-rate',
            startRate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 50,
            maxVUs: MAX_VUS,
            stages: stages[__ENV.TEST_TYPE || 'smoke'],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],

        'http_req_duration{phase:scenario,name:cart_add}': ['p(95)<800'],
        'http_req_duration{phase:scenario,name:order_create}': ['p(95)<1000'],
        'http_req_duration{phase:scenario,name:payment_create}': ['p(95)<1500'],
        'http_req_failed{phase:scenario}': ['rate<0.01'],
    },
};

export function setup() {
    // 1. 상품 ID 수집 (인증 불필요)
    const productRes = http.get(`${BASE_URL}/products?page=0&size=20`,
        {
            tags: {
                phase: 'setup',
                name: 'product_list'
            },
        }
    );

    check(productRes, {
        'product fetch success': (r) => r.status === 200,
    });

    if (productRes.status !== 200) {
        throw new Error(`Product fetch failed: status=${productRes.status}`);
    }

    const products = productRes.json('data.content');

    if (!Array.isArray(products) || products.length === 0) {
        throw new Error('No products found. Seed products before running test.');
    }

    const productIds = products.map(p => p.id);

    // 2. 로그인 후 배송지 ID 수집
    const {tokens} = loginUsers(MAX_VUS);

    const users = tokens.map(token => {
        const addrRes = http.get(`${BASE_URL}/addresses`, {
            ...authHeaders(token),
            tags: {
                phase: 'setup',
                name: 'address_fetch'
            }
        }
        );

        check(addrRes, {
            'address fetch success': (r) => r.status === 200,
        });

        if (addrRes.status !== 200) {
            throw new Error(`Address fetch failed: status=${addrRes.status}`);
        }

        const addresses = addrRes.json('data');

        if (!addresses || addresses.length === 0) {
            throw new Error('Address missing. Seed addresses first');
        }

        const addressId = addresses[0].addressId;

        if (!addressId) {
            throw new Error('Address missing for token. Seed addresses first.');
        }

        return {token, addressId};
    });

    return {users, productIds};
}

// GET에는 authHeaders(token) 그대로 사용,
// POST에는 Content-Type을 추가로 병합
function jsonAuth(token) {
    return {
        headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };
}

export default function (data) {
    const user = data.users[(__VU - 1) % data.users.length];
    const productId = data.productIds[Math.floor(Math.random() * data.productIds.length)];

    // 1. 장바구니에 상품 추가
    // sort=createdAt,desc&size=1 → 응답에서 방금 추가한 항목의 cartItemId를 바로 꺼냄
    const cartRes = http.post(
        `${BASE_URL}/carts/items?sort=createdAt,desc&size=1`,
        JSON.stringify({productId, quantity: 1}),
        {
            ...jsonAuth(user.token),
            tags: {
                phase: 'scenario',
                name: 'cart_add'
            },
        }
    );

    check(cartRes, {'cart item added 201': (r) => r.status === 201});
    if (cartRes.status !== 201) {
        const bodyPreview = (cartRes.body || '').slice(0, 300);
        console.error(`CART FAILED: status = ${cartRes.status}, bodyPreview = ${bodyPreview}`);

        return;
    }

    const body = cartRes.json();
    const cartItemId = body.data.items.content[0].id;

    // 2. 주문 생성
    const orderRes = http.post(
        `${BASE_URL}/orders`,
        JSON.stringify({cartItemIds: [cartItemId], addressId: user.addressId}),
        {
            ...jsonAuth(user.token),
            tags: {
                phase: 'scenario',
                name: 'order_create'
            },
        }
    );

    check(orderRes, {'order created 201': (r) => r.status === 201});
    if (orderRes.status !== 201) {
        const bodyPreview = (orderRes.body || '').slice(0, 300);
        console.error(`ORDER FAILED: status = ${orderRes.status}, bodyPreview = ${bodyPreview}`)

        return;
    }

    const orderId = orderRes.json('data.orderId');

    // 3. 결제
    const paymentPayload = JSON.stringify({
        orderId: orderId,
        method: 'MOCK'
    });

    const paymentRes = http.post(
        `${BASE_URL}/payments`,
        paymentPayload,
        {
            ...jsonAuth(user.token),
            tags: {
                phase: 'scenario',
                name: 'payment_create'
            },
        }
    );

    check(paymentRes, {'payment processed 201': (r) => r.status === 201});

    if (paymentRes.status !== 201) {
        const bodyPreview = (paymentRes.body || '').slice(0, 300);
        console.error(`PAYMENT FAILED: status = ${paymentRes.status}, bodyPreview = ${bodyPreview}`);
    }
}