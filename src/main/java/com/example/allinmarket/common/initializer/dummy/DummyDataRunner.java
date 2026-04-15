package com.example.allinmarket.common.initializer.dummy;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("local")
public class DummyDataRunner implements ApplicationRunner {

    private final DummyDataService dummyDataService;
    private final DummyProperties properties;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        if(!properties.isEnabled()) return;

        dummyDataService.createDummyCategory(properties.getCategoryCount());
        dummyDataService.createDummySeller(properties.getSellerCount());
        dummyDataService.createDummyProduct(properties.getProductCount());
        dummyDataService.createDummyBuyer(properties.getBuyerCartCount());
        dummyDataService.createDummyCart(properties.getBuyerCartCount());
        dummyDataService.createDummyAddress(properties.getAddressCount());
    }
}