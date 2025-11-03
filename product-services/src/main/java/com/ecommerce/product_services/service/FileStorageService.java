package com.ecommerce.product_services.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path rootLocation;

    public FileStorageService() {
        this.rootLocation = Paths.get("product-images");
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }

            
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            
            String uniqueFilename = UUID.randomUUID().toString() + extension;

           
            Path destinationFile = this.rootLocation.resolve(Paths.get(uniqueFilename))
                    .normalize().toAbsolutePath();

            Files.copy(file.getInputStream(), destinationFile);

          
            return "/images/" + uniqueFilename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return; 
        }

        try {
            
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path filePath = this.rootLocation.resolve(filename).normalize().toAbsolutePath();

            Files.deleteIfExists(filePath);

        } catch (IOException e) {
            System.err.println("Could not delete file: " + imageUrl + ". Error: " + e.getMessage());
        }
    }
}