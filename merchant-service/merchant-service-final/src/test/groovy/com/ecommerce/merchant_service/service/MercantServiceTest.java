package com.ecommerce.merchant_service.service;

import com.ecommerce.merchant_service.dto.MerchantResponse;
import com.ecommerce.merchant_service.entity.Merchant;
import com.ecommerce.merchant_service.mongo.MerchantStatsRepository;
import com.ecommerce.merchant_service.repository.MerchantRepository;
import com.ecommerce.merchant_service.security.JwtValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MerchantServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private MerchantStatsRepository statsRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private JwtValidator jwtValidator;

    @InjectMocks private MerchantService merchantService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetMerchantById_Success() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setUserId(10L);

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("name", "John", "email", "john@test.com", "role", "MERCHANT"));

        MerchantResponse response = merchantService.getMerchantById(1L);

        assertEquals(1L, response.getMerchantId());
        assertEquals(10L, response.getUserId());
        assertEquals("John", response.getName());
    }

    @Test
    void testCreateMerchant_Success() {
        Merchant merchant = new Merchant();
        merchant.setUserId(99L);

        when(jwtValidator.validateToken(anyString())).thenReturn(Map.of("role", "MERCHANT"));
        when(merchantRepository.save(any(Merchant.class))).thenReturn(merchant);

        Merchant created = merchantService.createMerchant(merchant, "Bearer token");

        assertEquals(99L, created.getUserId());
        verify(merchantRepository, times(1)).save(merchant);
    }

    @Test
    void testCreateMerchant_AccessDenied() {
        Merchant merchant = new Merchant();

        when(jwtValidator.validateToken(anyString())).thenReturn(Map.of("role", "CUSTOMER"));

        assertThrows(RuntimeException.class,
                () -> merchantService.createMerchant(merchant, "Bearer token"));
    }
}