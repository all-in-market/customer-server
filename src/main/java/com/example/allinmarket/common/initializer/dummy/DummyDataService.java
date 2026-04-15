package com.example.allinmarket.common.initializer.dummy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DummyDataService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;


    public void createDummyCategory(int categoryCount) {

    }
}
