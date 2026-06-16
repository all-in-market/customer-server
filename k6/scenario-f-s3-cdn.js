import http from 'k6/http';
import { check } from 'k6';

const TARGET = __ENV.TARGET || 'cdn'; // s3 | cdn

const s3Urls = [
    'https://all-in-market-product-images-323215070808.s3.ap-northeast-2.amazonaws.com/products/1/6242acc9-1eab-4a0e-a25d-f04b5c040189-white-round-stand-podium-placing-products-3d-background.jpg',
    'https://all-in-market-product-images-323215070808.s3.ap-northeast-2.amazonaws.com/products/2/ebc2012b-72a5-4288-ab9e-e355cc3bb874-product_1.jpg',
    'https://all-in-market-product-images-323215070808.s3.ap-northeast-2.amazonaws.com/products/3/45f87579-8df9-4954-9b1d-ce31863fede6-product_2.jpg',
    'https://all-in-market-product-images-323215070808.s3.ap-northeast-2.amazonaws.com/products/4/b9e217b0-b4e3-4a98-961c-082045f8b680-product_3.jpg',
    'https://all-in-market-product-images-323215070808.s3.ap-northeast-2.amazonaws.com/products/5/7c8985a2-9c38-4e47-b3f6-8adfc11e816e-product_4.jpg',
    'https://all-in-market-product-images-323215070808.s3.ap-northeast-2.amazonaws.com/products/6/a969202b-5afa-48ac-af29-e4930c9a21fa-product_5.jpg',
    'https://all-in-market-product-images-323215070808.s3.ap-northeast-2.amazonaws.com/products/7/7ad280d0-9e71-4c00-bcd1-e17880cb10d7-product_6.jpg',
    'https://all-in-market-product-images-323215070808.s3.ap-northeast-2.amazonaws.com/products/8/1324b81b-8aac-4067-89b6-ab63cbc231b2-product_7.jpg',
    'https://all-in-market-product-images-323215070808.s3.ap-northeast-2.amazonaws.com/products/9/1e52d887-0b2b-4f96-a8e0-cf16fd795914-product_8.jpg',
    'https://all-in-market-product-images-323215070808.s3.ap-northeast-2.amazonaws.com/products/10/26676490-4343-49ff-a65f-78194d3a7e53-product_9.jpg',
];

const cdnUrls = [
    'https://cdn.hyu1335.cloud/products/1/6242acc9-1eab-4a0e-a25d-f04b5c040189-white-round-stand-podium-placing-products-3d-background.jpg',
    'https://cdn.hyu1335.cloud/products/2/ebc2012b-72a5-4288-ab9e-e355cc3bb874-product_1.jpg',
    'https://cdn.hyu1335.cloud/products/3/45f87579-8df9-4954-9b1d-ce31863fede6-product_2.jpg',
    'https://cdn.hyu1335.cloud/products/4/b9e217b0-b4e3-4a98-961c-082045f8b680-product_3.jpg',
    'https://cdn.hyu1335.cloud/products/5/7c8985a2-9c38-4e47-b3f6-8adfc11e816e-product_4.jpg',
    'https://cdn.hyu1335.cloud/products/6/a969202b-5afa-48ac-af29-e4930c9a21fa-product_5.jpg',
    'https://cdn.hyu1335.cloud/products/7/7ad280d0-9e71-4c00-bcd1-e17880cb10d7-product_6.jpg',
    'https://cdn.hyu1335.cloud/products/8/1324b81b-8aac-4067-89b6-ab63cbc231b2-product_7.jpg',
    'https://cdn.hyu1335.cloud/products/9/1e52d887-0b2b-4f96-a8e0-cf16fd795914-product_8.jpg',
    'https://cdn.hyu1335.cloud/products/10/26676490-4343-49ff-a65f-78194d3a7e53-product_9.jpg',
];

export const options = {
    scenarios: {
        image_compare_test: {
            executor: 'constant-arrival-rate',
            rate: 10,
            timeUnit: '1s',
            duration: '5m',
            preAllocatedVUs: 300,
            maxVUs: 1000,
        },
    },

    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
    },
};

/**
 * CDN 캐시 웜업
 * CDN 테스트일 때만 실행
 * 테스트 시작 전에 모든 이미지 1회 조회
 */
export function setup() {
    if (TARGET !== 'cdn') {
        return;
    }

    console.log('CDN cache warming start...');

    for (const url of cdnUrls) {
        const res = http.get(url);

        check(res, {
            'warmup status 200': (r) => r.status === 200,
        });

        console.log(
            `Warmup URL=${url} X-Cache=${res.headers['X-Cache']}`
        );
    }

    console.log('CDN cache warming complete.');
}

export default function () {
    const urls = TARGET === 's3' ? s3Urls : cdnUrls;

    const url = urls[Math.floor(Math.random() * urls.length)];

    const res = http.get(url);

    check(res, {
        'status is 200': (r) => r.status === 200,

        'content exists': (r) =>
            Number(r.headers['Content-Length'] || 0) > 0 ||
            r.body.length > 0,
    });

    // CDN 테스트일 경우 CloudFront 헤더 확인
    if (TARGET === 'cdn') {
        check(res, {
            'cloudfront cache header exists': (r) =>
                r.headers['X-Cache'] !== undefined,
        });
    }
}