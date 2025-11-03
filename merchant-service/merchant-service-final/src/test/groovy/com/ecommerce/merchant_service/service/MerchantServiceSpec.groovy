package com.ecommerce.merchant_service.service

import com.ecommerce.merchant_service.entity.Merchant
import com.ecommerce.merchant_service.mongo.MerchantStatsRepository
import com.ecommerce.merchant_service.repository.MerchantRepository
import com.ecommerce.merchant_service.security.JwtValidator
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class MerchantServiceSpec extends Specification {

    def merchantRepository = Mock(MerchantRepository)
    def statsRepository = Mock(MerchantStatsRepository)
    def restTemplate = Mock(RestTemplate)
    def jwtValidator = Mock(JwtValidator)

    def service = new MerchantService(merchantRepository, statsRepository, restTemplate, jwtValidator)

    def "should return merchant details by ID"() {
        given:
        def merchant = new Merchant(id: 1L, userId: 99L)
        merchantRepository.findById(1L) >> Optional.of(merchant)
        restTemplate.getForObject(_, Map) >> [name: 'John', email: 'john@test.com', role: 'MERCHANT']

        when:
        def result = service.getMerchantById(1L)

        then:
        result.merchantId == 1L
        result.userId == 99L
        result.name == 'John'
    }

    def "should allow merchant creation only for role MERCHANT"() {
        given:
        def merchant = new Merchant(userId: 50L)
        jwtValidator.validateToken(_) >> [role: 'MERCHANT']
        merchantRepository.save(_) >> merchant

        when:
        def created = service.createMerchant(merchant, "Bearer test")

        then:
        created.userId == 50L
    }

    def "should deny merchant creation for non-merchant role"() {
        given:
        def merchant = new Merchant(userId: 10L)
        jwtValidator.validateToken(_) >> [role: 'CUSTOMER']

        when:
        service.createMerchant(merchant, "Bearer test")

        then:
        thrown(RuntimeException)
    }
}