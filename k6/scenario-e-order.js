import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { loginUsers, authHeaders } from './common.js';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const MAX_VUS = parseInt(__ENV.MAX_VUS || '500');

// 주문 생성 응답시간
const orderDuration = new Trend('order_duration_ms', true);

// 주문 성공률
const orderSuccessRate = new Rate('order_success_rate');

// 주문 실패 건수
const orderFailureCount = new Counter('order_failure_count');

// 재고 부족 건수
const outOfStockCount = new Counter('out_of_stock_count');

// 중복 주문 건수
const duplicateOrderCount = new Counter('duplicate_order_count');

const stages = {
    smoke: [{ target: 5, duration: '30s' }],
    load: [
        { target: 50, duration: '1m' },
        { target: 50, duration: '5m' },
        { target: 0, duration: '30s' },
    ],
    stress: [
        { target: 50, duration: '1m' },
        { target: 150, duration: '1m' },
        { target: 300, duration: '1m' },
        { target: 600, duration: '1m' },
        { target: 0, duration: '30s' },
    ],
};

export const options = {
    scenarios: {
        order_only: {
            executor: 'ramping-arrival-rate',
            startRate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 50,
            maxVUs: MAX_VUS,
            stages: stages[__ENV.TEST_TYPE || 'smoke'],
        },
    },

    thresholds: {
        // 전체 HTTP 실패율
        http_req_failed: [
            'rate<0.01'
        ],

        // 주소 조회 성능
        'http_req_duration{name:address_fetch}': [
            'p(95)<500',
            'avg<300',
        ],

        // 장바구니 조회 성능
        'http_req_duration{name:cart_fetch}': [
            'p(95)<700',
            'avg<500',
        ],

        // 주문 생성 API 성능
        'http_req_duration{name:order_create}': [
            'p(95)<1000',
            'avg<700',
        ],

        // 커스텀 주문 메트릭
        'order_duration_ms': [
            'p(95)<1000',
            'avg<700',
        ],

        // 주문 성공율
        'order_success_rate': [
            'rate>0.99'
        ],

        // 주문 생성 실패 횟수 제한
        'order_failure_count': [
            'count<100'
        ],

        // 재고 부족 발생 횟수 제한
        'out_of_stock_count': [
            'count<50'
        ],

        // 중복 주문 발생 횟수 제한
        'duplicate_order_count': [
            'count<50'
        ],
    },
};

function jsonAuth(token, name) {
    return {
        headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
        tags: { name },
    };
}

export function setup() {
    const { tokens } = loginUsers(MAX_VUS);

    if (!tokens || tokens.length === 0) {
        throw new Error('No login tokens');
    }

    return {tokens};
}

// 주문 생성
export default function (data) {

    // 현재 사용자 선택
    const token = data.tokens[(__VU - 1) % data.tokens.length];

    // 주소 조회
    const addressRes = http.get(
        `${BASE_URL}/addresses`,
        {
            ...authHeaders(token),
            tags: {
                name: 'address_fetch',
            },
        }
    );

    if (addressRes.status !== 200) {
        console.error(`ADDRESS FETCH FAIL: ${addressRes.status}`);
        return;
    }

    const addresses = addressRes.json('data');

    if (!addresses || addresses.length === 0) {
        console.error('No addresses found');
        return;
    }

    const addressId = addresses[0].addressId;

    // 장바구니 조회
    const cartRes = http.get(
        `${BASE_URL}/carts?page=0&size=20`,
        {
            ...authHeaders(token),
            tags: {
                name: 'cart_fetch',
            },
        }
    );

    if (cartRes.status !== 200) {
        console.error(`CART FETCH FAIL: ${cartRes.status}`);
        return;
    }

    const cartItems =
        cartRes.json('data.items.content');

    if (!cartItems || cartItems.length === 0) {
        console.error('No cart items found');
        return;
    }

    const cartItem = cartItems[Math.floor(Math.random() * cartItems.length)];

    const cartItemId = cartItem.id;

    // 주문 생성 시작
    const start = Date.now();

    // 주문 생성 API 호출
    const orderRes = http.post(
        `${BASE_URL}/orders`,
        JSON.stringify({
            cartItemIds: [cartItemId],
            addressId,
        }),
        jsonAuth(token, 'order_create')
    );

    // 응답 시간 기록
    orderDuration.add(Date.now() - start);

    // 성공 여부 기록
    const success =
        orderRes.status === 201;

    if (success) {
        console.log(
            `[ORDER SUCCESS]
            cartItemId=${cartItemId}`
        );
    }

    orderSuccessRate.add(success);

    check(orderRes, {
        'order created 201':
            (r) => r.status === 201
    });

    // 실패 처리
    if (!success) {

        orderFailureCount.add(1);

        const bodyPreview =
            (orderRes.body || '').slice(0, 300);

        // 재고 부족 감지
        if (
            orderRes.status === 409 &&
            (
                bodyPreview.includes('재고') ||
                bodyPreview.includes('stock')
            )
        ) {

            outOfStockCount.add(1);

            console.warn(
                `[OUT OF STOCK]
                cartItemId=${cartItemId}
                status=${orderRes.status}
                body=${bodyPreview}`
            );
            return;
        }

        // 중복 주문 감지
        if (
            orderRes.status === 409 &&
            (
                bodyPreview.includes('중복') ||
                bodyPreview.includes('duplicate')
            )
        ) {

            duplicateOrderCount.add(1);

            console.warn(
                `[DUPLICATE ORDER]
                cartItemId=${cartItemId}
                status=${orderRes.status}
                body=${bodyPreview}`
            );
            return;
        }

        // 일반 실패 로그
        console.error(
            `[ORDER FAIL]
            cartItemId=${cartItemId}
            status=${orderRes.status}
            body=${bodyPreview}`
        );
    }
}