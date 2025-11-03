package com.ecommerce.merchant_service.service;

import com.ecommerce.merchant_service.dto.MerchantResponse;
import com.ecommerce.merchant_service.entity.Merchant;
import com.ecommerce.merchant_service.mongo.MerchantStats;
import com.ecommerce.merchant_service.mongo.MerchantStatsRepository;
import com.ecommerce.merchant_service.repository.MerchantRepository;
import com.ecommerce.merchant_service.security.JwtValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantStatsRepository statsRepository;
    private final RestTemplate restTemplate;
    private final JwtValidator jwtValidator;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Autowired
    public MerchantService(MerchantRepository merchantRepository,
                           MerchantStatsRepository statsRepository,
                           RestTemplate restTemplate,
                           JwtValidator jwtValidator) {
        this.merchantRepository = merchantRepository;
        this.statsRepository = statsRepository;
        this.restTemplate = restTemplate;
        this.jwtValidator = jwtValidator;
    }

    public MerchantResponse getMerchantById(Long id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        String url = userServiceUrl + "/users/" + merchant.getUserId();
        Map<String, Object> userMap = restTemplate.getForObject(url, Map.class);

        MerchantResponse response = new MerchantResponse();
        response.setMerchantId(merchant.getId());
        response.setUserId(merchant.getUserId());

        if (userMap != null) {
            response.setName((String) userMap.get("name"));
            response.setEmail((String) userMap.get("email"));
            response.setRole((String) userMap.get("role"));
        }

        return response;
    }

    public MerchantResponse getMerchantByUserId(Long userId) {
        Merchant merchant = merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Merchant not found for userId: " + userId));

        String url = userServiceUrl + "/users/" + merchant.getUserId();
        Map<String, Object> userMap = restTemplate.getForObject(url, Map.class);

        MerchantResponse response = new MerchantResponse();
        response.setMerchantId(merchant.getId());
        response.setUserId(merchant.getUserId());

        if (userMap != null) {
            response.setName((String) userMap.get("name"));
            response.setEmail((String) userMap.get("email"));
            response.setRole((String) userMap.get("role"));
        }

        return response;
    }

    public Merchant updateMerchant(Long id, Merchant data, String authHeader) {
        Map<String, Object> claims = jwtValidator.validateToken(authHeader);

        String role = (String) claims.get("role");
        if (!"MERCHANT".equals(role) && !"ADMIN".equals(role)) {
            throw new RuntimeException("Access denied: Only merchants or admins can update merchant info");
        }

        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));


        return merchantRepository.save(merchant);
    }

    public Merchant createMerchant(Merchant merchant, String authHeader) {
        Map<String, Object> claims = jwtValidator.validateToken(authHeader);
        String role = (String) claims.get("role");

        if (!"MERCHANT".equals(role)) {
            throw new RuntimeException("Only merchants can register merchant accounts");
        }

        return merchantRepository.save(merchant);
    }

    public Optional<MerchantStats> getMerchantStats(Long merchantId) {
        return statsRepository.findByMerchantId(merchantId);
    }
}
