package com.ecommerce.merchant_service

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest (properties = "spring.profiles.active=test")
class MerchantServiceApplicationSpec extends Specification {

	def "context loads successfully"() {
		expect:
		true
	}
}
