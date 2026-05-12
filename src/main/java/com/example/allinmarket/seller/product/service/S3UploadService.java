package com.example.allinmarket.seller.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cdn.url}")
    private String productImageCdnUrl;

    public String uploadProductImage(MultipartFile file, Long productId) {

        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                        ? file.getOriginalFilename()
                        : "unnamed";

        String extension = StringUtils.getFilenameExtension(originalFilename);
        String safeName = UUID.randomUUID() + (StringUtils.hasText(extension) ? "." + extension.toLowerCase() : "");

        String key = "products/" + productId + "/" + safeName;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(file.getBytes())
            );

            return "https://" + productImageCdnUrl + "/" + key;

        } catch (IOException e) {
            throw new RuntimeException("상품 이미지 업로드에 실패했습니다.", e);
        }
    }
}