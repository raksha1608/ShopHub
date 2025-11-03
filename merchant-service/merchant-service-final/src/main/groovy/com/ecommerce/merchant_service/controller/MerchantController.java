package com.ecommerce.merchant_service.controller;

import com.ecommerce.merchant_service.dto.MerchantResponse;
import com.ecommerce.merchant_service.entity.Merchant;
import com.ecommerce.merchant_service.mongo.MerchantStats;
import com.ecommerce.merchant_service.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/merchants")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    @GetMapping("/{id}")
    public MerchantResponse getMerchant(@PathVariable Long id) {
        return merchantService.getMerchantById(id);
    }

    @GetMapping("/user/{userId}")
    public MerchantResponse getMerchantByUserId(@PathVariable Long userId) {
        return merchantService.getMerchantByUserId(userId);
    }

    @PutMapping("/{id}")
    public Merchant updateMerchant(
            @PathVariable Long id,
            @RequestBody Merchant merchant,
            @RequestHeader("Authorization") String authHeader
    ) {
        return merchantService.updateMerchant(id, merchant, authHeader);
    }

    @PostMapping("/register")
    public Merchant createMerchant(
            @RequestBody Merchant merchant,
            @RequestHeader("Authorization") String authHeader
    ) {
        return merchantService.createMerchant(merchant, authHeader);
    }

    @GetMapping("/{merchantId}/stats")
    public Optional<MerchantStats> getMerchantStats(@PathVariable Long merchantId) {
        return merchantService.getMerchantStats(merchantId);
    }
}
