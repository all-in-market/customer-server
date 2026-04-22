package com.example.allinmarket;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class AllInMarketApplicationTests {

    @MockitoBean
    RedissonClient redissonClient;

    @Test
    void contextLoads() {
    }

}
