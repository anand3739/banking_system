package com.example.banking.controller;

import com.example.banking.dto.fraud.FraudAlertResponse;
import com.example.banking.service.FraudAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fraud-alerts")
@RequiredArgsConstructor
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    @GetMapping
    public ResponseEntity<List<FraudAlertResponse>> getFraudAlerts() {
        return ResponseEntity.ok(fraudAlertService.getAllFraudAlerts());
    }
}
