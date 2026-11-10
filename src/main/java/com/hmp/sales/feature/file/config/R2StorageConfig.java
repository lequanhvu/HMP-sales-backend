package com.hmp.sales.feature.file.config;

import com.hmp.sales.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Cấu hình kết nối AWS S3Client trỏ đến Cloudflare R2 Object Storage.
 * <p>
 * Cloudflare R2 tương thích 100% với chuẩn S3 API.
 * Bean {@link S3Client} chỉ được nạp vào Spring context khi thuộc tính
 * {@code app.storage.r2.enabled = true}.
 */
@Slf4j
@Configuration
public class R2StorageConfig {

    /**
     * Khởi tạo Bean S3Client kết nối tới Cloudflare R2 dựa trên AppProperties.
     *
     * @param appProperties cấu hình ứng dụng chứa thông tin tài khoản và bucket R2
     * @return đối tượng {@link S3Client} kết nối R2
     */
    @Bean
    @ConditionalOnProperty(name = "app.storage.r2.enabled", havingValue = "true")
    public S3Client r2S3Client(AppProperties appProperties) {
        AppProperties.R2Properties r2 = appProperties.storage().r2();
        log.info("[R2StorageConfig] Khởi tạo kết nối Cloudflare R2: bucket={}, endpoint=https://{}.r2.cloudflarestorage.com",
                r2.bucketName(), r2.accountId());

        return S3Client.builder()
                .endpointOverride(URI.create("https://" + r2.accountId() + ".r2.cloudflarestorage.com"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.accessKey(), r2.secretKey())))
                .region(Region.of("auto"))
                .build();
    }
}

