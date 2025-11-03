package com.example.email_service.service;

import com.example.email_service.dto.OrderItemsDto;
import com.example.email_service.dto.OrderPlacedEventDto;
import com.example.email_service.dto.ProductDto;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class EmailServiceTest {


    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RestTemplate restTemplate;


    @InjectMocks
    private EmailService emailService;


    @BeforeEach
    void setUp() {

        Mockito.reset(mailSender, restTemplate);

        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@ecommerce.com");
        ReflectionTestUtils.setField(emailService, "productServiceUrl", "http://fake-product-service.com");
    }


    @Test
    void sendOrderConfirmationEmail_shouldSendEmailSuccessfully() throws Exception {

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        OrderItemsDto item = new OrderItemsDto();
        item.setProductId("prod-123");
        item.setQuantity(1);
        item.setPrice(100.0);

        OrderPlacedEventDto event = new OrderPlacedEventDto();
        event.setOrderId(1L);
        event.setUserEmail("test@example.com");
        event.setTotalAmount(100.0);
        event.setItems(List.of(item));


        ProductDto fakeProduct = new ProductDto();
        fakeProduct.setName("Test Product");
        fakeProduct.setImageUrl("/images/test-product.png");


        when(restTemplate.getForObject(
                "http://fake-product-service.com/products/prod-123",
                ProductDto.class
        )).thenReturn(fakeProduct);


        Resource mockImageResource = mock(Resource.class);
        when(restTemplate.getForObject(
                "http://fake-product-service.com/images/test-product.png",
                Resource.class
        )).thenReturn(mockImageResource);



        emailService.sendOrderConfirmationEmail(event);





        verify(restTemplate, times(1)).getForObject(
                "http://fake-product-service.com/products/prod-123",
                ProductDto.class
        );


        verify(restTemplate, times(1)).getForObject(
                "http://fake-product-service.com/images/test-product.png",
                Resource.class
        );


        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());


        MimeMessage sentMessage = messageCaptor.getValue();
        assertEquals(mimeMessage, sentMessage);
    }

    @Test
    void sendOrderConfirmationEmail_shouldSendEmail_whenProductServiceFails() {

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);


        OrderItemsDto item = new OrderItemsDto();
        item.setProductId("prod-404");
        item.setQuantity(1);
        item.setPrice(100.0);

        OrderPlacedEventDto event = new OrderPlacedEventDto();
        event.setOrderId(2L);
        event.setUserEmail("test@example.com");
        event.setTotalAmount(100.0);
        event.setItems(List.of(item));


        when(restTemplate.getForObject(
                "http://fake-product-service.com/products/prod-404",
                ProductDto.class
        )).thenThrow(new RuntimeException("Product service is down"));



        emailService.sendOrderConfirmationEmail(event);




        verify(restTemplate, times(1)).getForObject(
                "http://fake-product-service.com/products/prod-404",
                ProductDto.class
        );


        verify(restTemplate, never()).getForObject(anyString(), eq(Resource.class));


        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}