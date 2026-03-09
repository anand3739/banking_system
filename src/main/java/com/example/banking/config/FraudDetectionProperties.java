package com.example.banking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "fraud")
public class FraudDetectionProperties {

    private BigDecimal amountThreshold = BigDecimal.valueOf(100000);
    private int txCountThreshold = 5;
    private int txWindowMinutes = 10;
    private Set<Long> blacklistedAccountIds = new HashSet<>();
}
