package com.ecommerce.product_services.service;

import com.ecommerce.product_services.model.Product;
import com.ecommerce.product_services.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private MultipartFile mockFile;

    @InjectMocks private ProductService productService;

    private Product sampleProduct;
    private Product.Merchant merchant;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        merchant = new Product.Merchant();
        merchant.setMerchant_id(101);
        merchant.setName("Merchant A");
        merchant.setPrice(100.0);
        merchant.setStock(50);

        sampleProduct = new Product();
        sampleProduct.setId("1");
        sampleProduct.setName("iPhone");
        sampleProduct.setBrand("Apple");
        sampleProduct.setCategory("Electronics");
        sampleProduct.setDescription("Smartphone");
        sampleProduct.setMerchants(List.of(merchant));
        sampleProduct.setAttributes(Map.of("color", "black"));
        sampleProduct.setImageUrl("/images/old.png");
    }

    @Test
    void addProduct_shouldSaveNewProductSuccessfully() {
        when(productRepository.findByNameIgnoreCase("iPhone")).thenReturn(Collections.emptyList());
        when(fileStorageService.store(mockFile)).thenReturn("/images/new.png");
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ResponseEntity<?> response = productService.addProduct(sampleProduct, mockFile);

        assertEquals(200, response.getStatusCode().value());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void addProduct_shouldThrowIfSameMerchantExists() {
        Product existing = new Product();
        existing.setName("iPhone");
        existing.setBrand("Apple");
        existing.setMerchants(new ArrayList<>(List.of(merchant)));

        when(productRepository.findByNameIgnoreCase("iPhone")).thenReturn(List.of(existing));

        Product.Merchant dup = new Product.Merchant();
        dup.setMerchant_id(101);
        dup.setName("Merchant A");
        sampleProduct.setMerchants(List.of(dup));

        ResponseEntity<?> response = productService.addProduct(sampleProduct, mockFile);
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("already added"));
    }


    @Test
    void addProduct_shouldFailForInvalidCategory() {
        sampleProduct.setCategory("InvalidCategory");
        ResponseEntity<?> response = productService.addProduct(sampleProduct, mockFile);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void addProduct_shouldFailForEmptyName() {
        sampleProduct.setName(" ");
        ResponseEntity<?> response = productService.addProduct(sampleProduct, mockFile);
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("Product name cannot be empty"));
    }

    @Test
    void addProduct_shouldHandleFileStorageException() {
        when(productRepository.findByNameIgnoreCase("iPhone")).thenReturn(Collections.emptyList());
        when(fileStorageService.store(mockFile)).thenThrow(new RuntimeException("storage failed"));

        ResponseEntity<?> response = productService.addProduct(sampleProduct, mockFile);

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("Failed to add product"));
    }

    @Test
    void getProductByIdResponse_shouldReturnOkIfExists() {
        when(productRepository.findById("1")).thenReturn(Optional.of(sampleProduct));
        ResponseEntity<?> response = productService.getProductByIdResponse("1");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getProductByIdResponse_shouldReturnNotFound() {
        when(productRepository.findById("1")).thenReturn(Optional.empty());
        ResponseEntity<?> response = productService.getProductByIdResponse("1");
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getAllProductsResponse_shouldReturnListByCategory() {
        when(productRepository.findByCategory("Electronics")).thenReturn(List.of(sampleProduct));
        ResponseEntity<?> response = productService.getAllProductsResponse("Electronics");
        assertEquals(200, response.getStatusCode().value());
        verify(productRepository).findByCategory("Electronics");
    }

    @Test
    void getAllProductsResponse_shouldReturnAllIfCategoryNull() {
        when(productRepository.findAll()).thenReturn(List.of(sampleProduct));
        ResponseEntity<?> response = productService.getAllProductsResponse(null);
        assertEquals(200, response.getStatusCode().value());
        verify(productRepository).findAll();
    }

    @Test
    void getAllProductsResponse_shouldReturnBadRequestForInvalidCategory() {
        ResponseEntity<?> response = productService.getAllProductsResponse("$$Invalid!!");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getAllProductsResponse_shouldReturnBadRequestForTooLongCategory() {
        String longCategory = "A".repeat(60);
        ResponseEntity<?> response = productService.getAllProductsResponse(longCategory);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getAllProductsResponse_shouldReturnInternalErrorOnException() {
        when(productRepository.findAll()).thenThrow(new RuntimeException("DB down"));
        ResponseEntity<?> response = productService.getAllProductsResponse("");
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void searchProductsResponse_shouldReturnOk() {
        when(productRepository.searchByMultipleFields("iphone")).thenReturn(List.of(sampleProduct));
        ResponseEntity<?> response = productService.searchProductsResponse("iphone");
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void searchProductsResponse_shouldReturnBadRequestForEmpty() {
        ResponseEntity<?> response = productService.searchProductsResponse("");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void searchProductsResponse_shouldReturnBadRequestForTooLongQuery() {
        String longQuery = "A".repeat(150);
        ResponseEntity<?> response = productService.searchProductsResponse(longQuery);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void searchProductsResponse_shouldReturnBadRequestForInvalidQuery() {
        ResponseEntity<?> response = productService.searchProductsResponse("$Invalid");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void searchProductsResponse_shouldReturnInternalErrorOnException() {
        when(productRepository.searchByMultipleFields("iphone")).thenThrow(new RuntimeException());
        ResponseEntity<?> response = productService.searchProductsResponse("iphone");
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void updateProductResponse_shouldUpdateImageIfProvided() {
        when(productRepository.findById("1")).thenReturn(Optional.of(sampleProduct));
        when(mockFile.isEmpty()).thenReturn(false);
        when(fileStorageService.store(mockFile)).thenReturn("/images/new.png");
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ResponseEntity<?> response = productService.updateProductResponse("1", sampleProduct, mockFile);
        assertEquals(200, response.getStatusCode().value());
        verify(fileStorageService).delete("/images/old.png");
    }

    @Test
    void updateProductResponse_shouldReturnNotFound() {
        when(productRepository.findById("1")).thenReturn(Optional.empty());
        ResponseEntity<?> response = productService.updateProductResponse("1", sampleProduct, mockFile);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleteProductResponse_shouldDeleteIfExists() {
        when(productRepository.findById("1")).thenReturn(Optional.of(sampleProduct));
        ResponseEntity<?> response = productService.deleteProductResponse("1");
        assertEquals(204, response.getStatusCode().value());
        verify(productRepository).deleteById("1");
    }

    @Test
    void deleteProductResponse_shouldReturnNotFoundIfMissing() {
        when(productRepository.findById("1")).thenReturn(Optional.empty());
        ResponseEntity<?> response = productService.deleteProductResponse("1");
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateStockResponse_shouldUpdateMerchantStock() {
        when(productRepository.findById("1")).thenReturn(Optional.of(sampleProduct));
        Map<String, Object> req = Map.of("productId", "1", "merchantId", 101, "quantity", 5);
        ResponseEntity<?> response = productService.updateStockResponse(req);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void updateStockResponse_shouldReturnNotFoundIfProductMissing() {
        when(productRepository.findById("1")).thenReturn(Optional.empty());
        Map<String, Object> req = Map.of("productId", "1", "merchantId", 101, "quantity", 5);
        ResponseEntity<?> response = productService.updateStockResponse(req);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateStockResponse_shouldReturnErrorIfMerchantMissing() {
        Product.Merchant m = new Product.Merchant();
        m.setMerchant_id(999);
        sampleProduct.setMerchants(List.of(m));

        when(productRepository.findById("1")).thenReturn(Optional.of(sampleProduct));
        Map<String, Object> req = Map.of("productId", "1", "merchantId", 101, "quantity", 5);

        ResponseEntity<?> response = productService.updateStockResponse(req);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateStockResponse_shouldReturnErrorOnException() {
        Map<String, Object> req = Map.of("productId", "1", "merchantId", "invalid", "quantity", 5);
        ResponseEntity<?> response = productService.updateStockResponse(req);
        assertEquals(500, response.getStatusCode().value());
    }
}
