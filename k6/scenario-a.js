import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

// 각 단계별 목표 RPS (iterations/s 기준, 1 iteration = 목록 + 상세 2건)
const stages = {
    smoke: [
        { target: 5,   duration: '30s' },
    ],
    load: [
        { target: 100, duration: '1m'  },  // 워밍업: 100 RPS까지 증가
        { target: 100, duration: '5m'  },  // 안정 상태 유지: 개선 전후 비교 기준값
        { target: 0,   duration: '30s' },  // 쿨다운
    ],
    stress: [
        { target: 20,  duration: '1m' },
        { target: 100,  duration: '1m' },
        { target: 200, duration: '1m' },
        { target: 600, duration: '1m' },
        { target: 1000, duration: '1m' },
        { target: 0,   duration: '30s' },
    ],
};

export const options = {
    scenarios: {
        product_browsing: {
            executor: 'ramping-arrival-rate',
            startRate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 100,
            maxVUs: 3000,
            stages: stages[__ENV.TEST_TYPE || 'smoke'],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000'],   // 95%의 요청이 1초 이내
        http_req_failed:   ['rate<0.01'],    // 에러율 1% 미만
    },
};

export default function () {
    // 1. 상품 목록 조회 (랜덤 페이지)
    const page = Math.floor(Math.random() * 100);
    const listRes = http.get(`${BASE_URL}/products?page=${page}&size=20`);

    check(listRes, {
        'product list 200': (r) => r.status === 200,
    });

    if (listRes.status !== 200) return;

    // 2. 목록 응답에서 productId 추출 후 상세 조회
    // 응답 구조: { data: { content: [{ id, name, ... }] } }
    const products = listRes.json('data.content');
    if (!products || products.length === 0) return;

    const randomProduct = products[Math.floor(Math.random() * products.length)];
    const detailRes = http.get(`${BASE_URL}/products/${randomProduct.id}`);

    check(detailRes, {
        'product detail 200': (r) => r.status === 200,
    });
}