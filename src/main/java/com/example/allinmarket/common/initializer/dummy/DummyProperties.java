package com.example.allinmarket.common.initializer.dummy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "dummy")
@Component
public class DummyProperties {
    private boolean enabled;
    private BatchType batchType;
    private int categoryCount;
    private int sellerCount;
    private int productCount;
    private int buyerCount;
    private int cartItemCount;
}
