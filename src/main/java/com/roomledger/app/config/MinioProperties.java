package com.roomledger.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
        @NotBlank String endpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String bucketPublic,
        @NotBlank String bucketPrivate,
        @NotBlank String publicBaseUrl
) { }
