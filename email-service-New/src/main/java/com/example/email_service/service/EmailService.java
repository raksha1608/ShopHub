package com.example.email_service.service;

import com.example.email_service.dto.OrderItemsDto;
import com.example.email_service.dto.OrderPlacedEventDto;
import com.example.email_service.dto.ProductDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${product.service.url}")
    private String productServiceUrl;


    @Async
    public void sendOrderConfirmationEmail(OrderPlacedEventDto event) {
        System.out.println("EmailService: Received job to send HTML email for order " + event.getOrderId());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(event.getUserEmail());
            helper.setSubject("Order Confirmation - #" + event.getOrderId());


            Map<String, ProductDto> productDetailsMap = new HashMap<>();
            for (OrderItemsDto item : event.getItems()) {
                ProductDto product = getProductDetails(item.getProductId());
                productDetailsMap.put(item.getProductId(), product);
            }

            String htmlBody = buildHtmlEmailBody(event, productDetailsMap);

            helper.setText(htmlBody, true);

            int itemIndex = 0;
            for (OrderItemsDto item : event.getItems()) {
                ProductDto product = productDetailsMap.get(item.getProductId());

                if (product != null && product.getImageUrl() != null) {
                    try {
                        String fullImageUrl = productServiceUrl + product.getImageUrl();
                        Resource imageResource = restTemplate.getForObject(fullImageUrl, Resource.class);

                        if (imageResource != null) {
                            String cid = "productImage" + itemIndex;
                            helper.addInline(cid, imageResource);
                        }
                    } catch (Exception e) {
                        System.err.println("Could not embed image for product " + item.getProductId() + ": " + e.getMessage());
                    }
                }
                itemIndex++;
            }

            mailSender.send(message);
            System.out.println("HTML Order confirmation email sent successfully to " + event.getUserEmail());

        } catch (MessagingException | MailException e) {
            System.err.println("Error sending HTML email: " + e.getMessage());
        }
    }



    private String buildHtmlEmailBody(OrderPlacedEventDto event, Map<String, ProductDto> productDetailsMap) {
        StringBuilder body = new StringBuilder();

        body.append("<html><body style='font-family: Arial, sans-serif;'>");
        body.append("<h2>Thank you for your order!</h2>");
        body.append("<p>Your order summary (ID: <b>").append(event.getOrderId()).append("</b>)</p>");

        body.append("<table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse; width: 100%;'>");
        body.append("<tr style='background-color: #f2f2f2;'>");
        body.append("<th>Image</th><th>Product</th><th>Quantity</th><th>Price</th>");
        body.append("</tr>");

        int itemIndex = 0;
        for (OrderItemsDto item : event.getItems()) {
            ProductDto product = productDetailsMap.get(item.getProductId());
            String productName = (product != null) ? product.getName() : "Product ID: " + item.getProductId();

            body.append("<tr>");

            body.append("<td style='text-align: center;'>");
            if (product != null && product.getImageUrl() != null) {
                body.append("<img src='cid:productImage").append(itemIndex).append("' width='100' alt='Product Image'/>");
            } else {
                body.append("No Image");
            }
            body.append("</td>");

            body.append("<td>").append(productName).append("</td>");
            body.append("<td style='text-align: center;'>").append(item.getQuantity()).append("</td>");
            body.append("<td style='text-align: right;'>₹").append(item.getPrice()).append("</td>");
            body.append("</tr>");
            itemIndex++;
        }

        body.append("</table>");

        body.append("<h3 style='text-align: right; margin-top: 20px;'>");
        body.append("TOTAL AMOUNT: ₹").append(event.getTotalAmount());
        body.append("</h3>");

        body.append("<p>Thank you for shopping with us!</p>");
        body.append("</body></html>");

        return body.toString();
    }


    private ProductDto getProductDetails(String productId) {
        try {
            ProductDto productData = restTemplate.getForObject(
                    productServiceUrl + "/products/" + productId,
                    ProductDto.class
            );
            return productData;

        } catch (Exception e) {
            System.err.println("Could not fetch product details for ID " + productId);
            System.err.println("   Error: " + e.getMessage());
            return null;
        }
    }
}