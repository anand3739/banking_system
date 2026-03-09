package com.example.banking.dto.customer;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class CustomerResponse {
    Long customerId;
    String name;
    String email;
    String phone;
    LocalDateTime createdAt;
}
