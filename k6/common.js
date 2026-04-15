import http from 'k6/http';

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

        tokens.push(res.json('data.accessToken'));
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
