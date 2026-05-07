import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { authHeaders, loginUsers } from './common.js';

const paymentResponseCallback = http.expectedStatuses(201, 409);

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

// 최대 Virtual User 수
const MAX_VUS = parseInt(__ENV.MAX_VUS || '500');

// 중복 결제 발생 추적용 Counter
// 이미 결제된 주문, 중복 결제 시도, 멱등성 처리 결과 확인, Lock 정상 동작 여부 확인
const duplicatePaymentCount =
    new Counter('duplicate_payment_count');


// 결제 API 전용 메트릭
const paymentDuration =
    new Trend('payment_duration_ms', true);

const stages = {
    smoke: [{ target: 5, duration: '30s' }],
    load: [
        { target: 100, duration: '1m' },
        { target: 100, duration: '5m' },
        { target: 0, duration: '30s' },
    ],
    stress: [
        { target: 100, duration: '1m' },
        { target: 300, duration: '1m' },
        { target: 600, duration: '1m' },
        { target: 1000, duration: '1m' },
        { target: 0, duration: '30s' },
    ],
};

// 시나리오 정의
export const options = {
    scenarios: {
        payment_only: {
            executor: 'ramping-arrival-rate',
            startRate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 100,
            maxVUs: MAX_VUS,
            stages: stages[__ENV.TEST_TYPE || stages.smoke],
        },
    },

    thresholds: {
        http_req_failed: [
            // 전체 HTTP 실패율
            'rate<0.01'
        ],

        // 주문 조회 API 응답 시간
        'http_req_duration{name:order_fetch}': [
            'p(95)<800'
        ],

        // 결제 API 응답 시간
        'http_req_duration{name:payment_create}': [
            'p(95)<2000'
        ],

        // 커스텀 결제 레이턴시
        'payment_duration_ms': [
            'p(95)<2000'
        ],

        // 결제 API 실패율
        'http_req_failed{name:payment_create}': [
            'rate<0.01'
        ],

        // 중복 결제 허용 범위
        'duplicate_payment_count': [
            'count<100'
        ],
    },
};

function jsonAuth(token, name) {
    return {
        headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
        tags: { name, },
    };
}

export function setup() {
    const {tokens} = loginUsers(MAX_VUS);

    if (!tokens || tokens.length === 0) {
        throw new Error('No login tokens');
    }

    return {tokens};
}

// 실제 부하 테스트 루프
export default function (data) {
    // 현재 VU에 대응되는 토큰 선택 (VU별 고정 사용자처럼 동작)
    const token = data.tokens[(__VU - 1) % data.tokens.length];

    const orderRes = http.get(
        `${BASE_URL}/orders?status=CREATED&page=0&size=20`,
        {
            ...authHeaders(token),
            tags: { name: 'order_fetch' },
        }
    );

    check(orderRes, {
        'order list 200': (r) => r.status === 200,
    });

    if (orderRes.status !== 200) {
        console.error(`ORDER FETCH FAILED: ${orderRes.status}`);
        console.error(orderRes.body);
        throw new Error('Failed to fetch orders');
    }

    const orders = orderRes.json('data.content');

    if (!orders || orders.length === 0) {
        console.error('No payment-ready orders found');
        return;
    }

    const order = orders[Math.floor(Math.random() * orders.length)];

    const orderId = order.orderId;

    const start = Date.now();

    const paymentRes = http.post(
        `${BASE_URL}/payments`,
        JSON.stringify({
            orderId,
            method: 'MOCK',
        }),
        {
            ...jsonAuth(token, 'payment_create'),
            responseCallback: paymentResponseCallback,
        }
    );

    // 결제 API 응답 시간 기록
    paymentDuration.add(Date.now() - start);

    check(paymentRes, {
        'payment processed': (r) =>
            r.status === 201 || r.status === 409,
    });

    if (paymentRes.status !== 201) {

        const bodyPreview =
            (paymentRes.body || '').slice(0, 300);

        // 중복 결제 감지
        if (
            paymentRes.status === 409 &&
            (
                bodyPreview.includes('이미 결제') ||
                bodyPreview.includes('already paid')
            )
        ) {

            duplicatePaymentCount.add(1);

            console.warn(
                `[DUPLICATE PAYMENT]
             orderId=${orderId}
             status=${paymentRes.status}
             body=${bodyPreview}`
            );

        } else {

            console.error(
                `[PAY FAIL]
             orderId=${orderId}
             status=${paymentRes.status}
             body=${bodyPreview}`
            );
        }
    }
}