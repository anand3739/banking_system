package com.example.banking.dto.common;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ErrorResponse {
    LocalDateTime timestamp;
    Integer status;
    String error;
    String message;
    String path;
    List<String> validationErrors;
}
