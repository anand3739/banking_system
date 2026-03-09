package com.example.banking.service;

import com.example.banking.dto.customer.CreateCustomerRequest;
import com.example.banking.dto.customer.CustomerResponse;
import com.example.banking.entity.Customer;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        Customer customer = Customer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    Customer getCustomerEntity(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + customerId));
    }

    private CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .customerId(customer.getCustomerId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
