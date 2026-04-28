import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = 'http://host.docker.internal:8080';

export function loginUsers(userCount) {
    const tokens = [];

    for (let i = 0; i < userCount; i++) {
        const email = `user${i}@test.com`;
        const password = '1234567890';

        const res = http.post(
            `${BASE_URL}/auth/login`,
            JSON.stringify({email, password}),
            {headers: {'Content-Type': 'application/json'}}
        );

        check(res, {
            'login success': (r) => r.status === 200,
        });

        if (res.status !== 200) {
            throw new Error(`Login failed: ${email}`);
        }

        const token = res.json('data.accessToken');

        if (!token) {
            throw new Error(`AccessToken missing: ${email}`);
        }

        tokens.push(token);
    }

    return { tokens };
}

export function authHeaders(token) {
    return {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    };
}
