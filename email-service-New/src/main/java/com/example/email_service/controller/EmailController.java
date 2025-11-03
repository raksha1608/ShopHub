package com.example.email_service.controller;

import com.example.email_service.dto.OrderPlacedEventDto;
import com.example.email_service.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/send-order-confirmation")
    public ResponseEntity<String> sendOrderConfirmation(
            @Valid @RequestBody OrderPlacedEventDto event) {

        emailService.sendOrderConfirmationEmail(event);


        return ResponseEntity.accepted().body("Email job accepted");
    }
}