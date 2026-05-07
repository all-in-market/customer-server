import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { loginUsers } from './common.js';

const CHAT_URL = __ENV.CHAT_URL || 'http://host.docker.internal:8082';

// 환경 변수 안전한 파싱
function parseEnvInt(envVar, defaultValue, minValue) {
    const value = __ENV[envVar];
    if (!value) return defaultValue;

    const parsed = Number.parseInt(value, 10);
    if (!Number.isFinite(parsed)) {
        throw new Error(`${envVar}="${value}" 는 유효한 정수가 아닙니다.`);
    }
    if (parsed < minValue) {
        throw new Error(`${envVar}=${parsed} 는 최소값 ${minValue}보다 작습니다.`);
    }
    return parsed;
}

const MAX_VUS = parseEnvInt('MAX_VUS', 50, 1);
const PRE_ALLOCATED_VUS = parseEnvInt('PRE_ALLOCATED_VUS', Math.min(20, Math.floor(MAX_VUS * 0.4)), 1);
const RUN_TAG = __ENV.RUN_TAG || 'default';

// 검증: preAllocatedVUs <= maxVUs
if (PRE_ALLOCATED_VUS > MAX_VUS) {
    throw new Error(
        `preAllocatedVUs (${PRE_ALLOCATED_VUS}) 는 maxVUs (${MAX_VUS}) 이하여야 합니다.`
    );
}

// 챗봇 응답 레이턴시 커스텀 메트릭
// Grafana 쿼리: SELECT percentile("value", 95) FROM "chat_duration_ms"
const chatDuration = new Trend('chat_duration_ms', true);

const stages = {
    smoke: [
        { target: 2,  duration: '30s' },
    ],
    load: [
        { target: 5,  duration: '1m'  },  // 워밍업
        { target: 10, duration: '3m'  },  // 안정 구간 — 가상 스레드 전/후 비교 기준
        { target: 0,  duration: '30s' },  // 쿨다운
    ],
    stress: [
        { target: 5,  duration: '1m' },
        { target: 10, duration: '1m' },
        { target: 20, duration: '1m' },
        { target: 30, duration: '1m' },
        { target: 0,  duration: '30s' },
    ],
};

const testType = __ENV.TEST_TYPE || 'smoke';
if (!stages[testType]) {
    throw new Error(
        `TEST_TYPE="${testType}" 은 유효하지 않습니다. ` +
        `유효값: ${Object.keys(stages).join(', ')}`
    );
}

export const options = {
    scenarios: {
        chat_flow: {
            executor: 'ramping-arrival-rate',
            startRate: 1,
            timeUnit: '1s',
            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,
            stages: stages[testType],
        },
    },
    tags: { run: RUN_TAG },
    thresholds: {
        // LLM 호출 포함이므로 기준 30초
        'http_req_duration{name:POST /chat/evaluate}': ['p(95)<30000'],
        'chat_duration_ms': ['p(95)<30000'],
        http_req_failed: ['rate<0.05'],
    },
};

// 테스트 질문 풀 — 반품/교환 정책 관련
const questions = [
    '반품 신청은 며칠 이내에 해야 하나요?',
    '단순 변심으로 반품할 때 배송비는 누가 내나요?',
    '식품은 반품이 가능한가요?',
    '환불은 얼마나 걸리나요?',
    '개봉한 화장품도 반품할 수 있나요?',
    '교환 신청은 언제까지 할 수 있나요?',
    '상품이 파손되어 왔을 때 교환이 되나요?',
    '주문 제작 상품도 교환할 수 있나요?',
    '교환 처리는 얼마나 걸리나요?',
    '잘못 배송된 상품은 반품과 교환 중 어떤 걸 신청해야 하나요?',
];

export function setup() {
    // 유저 1명만 로그인 — 챗봇은 인증된 사용자 1명으로 충분
    const { tokens } = loginUsers(1);
    if (!tokens || !tokens[0]) {
        throw new Error('setup 실패: 로그인 토큰을 획득하지 못했습니다.');
    }
    return { token: tokens[0] };
}

export default function (data) {
    if (!data || !data.token) {
        throw new Error('setup에서 제공된 데이터가 없습니다.');
    }

    const question = questions[Math.floor(Math.random() * questions.length)];

    const params = {
        headers: {
            Authorization: `Bearer ${data.token}`,
            'Content-Type': 'application/json',
        },
        tags: { name: 'POST /chat/evaluate' },
        timeout: '60s',
    };

    const start = Date.now();
    const res = http.post(
        `${CHAT_URL}/chat/evaluate`,
        JSON.stringify({ message: question }),
        params
    );
    chatDuration.add(Date.now() - start, { run: RUN_TAG });

    check(res, {
        'chat response 200': (r) => r.status === 200,
        'chat response not empty': (r) => r.body && r.body.length > 0,
    });
}