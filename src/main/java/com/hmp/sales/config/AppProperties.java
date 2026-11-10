package com.hmp.sales.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    JwtProperties jwt,
    OtpProperties otp,
    OrderProperties order,
    CorsProperties cors,
    PaginationProperties pagination,
    ProductProperties product,
    NotificationProperties notification,
    StorageProperties storage
) {
    @ConstructorBinding
    public AppProperties {
        if (pagination == null) {
            pagination = new PaginationProperties(0, 20, 100);
        }
        if (product == null) {
            product = new ProductProperties(1, 10);
        }
        if (notification == null) {
            notification = new NotificationProperties(
                new ZaloProperties(false, "", "", "", Map.of()),
                new EmailProperties(false, "", "HMP Sales", "")
            );
        }
        if (storage == null) {
            storage = new StorageProperties(new R2Properties(false, "", "", "", "", ""));
        }
    }

    public AppProperties(
        JwtProperties jwt,
        OtpProperties otp,
        OrderProperties order,
        CorsProperties cors,
        PaginationProperties pagination,
        ProductProperties product
    ) {
        this(jwt, otp, order, cors, pagination, product, null, null);
    }

    public AppProperties(
        JwtProperties jwt,
        OtpProperties otp,
        OrderProperties order,
        CorsProperties cors,
        PaginationProperties pagination,
        ProductProperties product,
        NotificationProperties notification
    ) {
        this(jwt, otp, order, cors, pagination, product, notification, null);
    }

    public record JwtProperties(
        String issuer,
        String secret,
        long expirationMinutes,
        long refreshExpirationDays
    ) {}

    public record OtpProperties(
        int expirySeconds,
        int maxAttempts,
        int resendIntervalSeconds
    ) {}

    public record OrderProperties(
        int cancellationWindowMinutes,
        int undoCancelWindowMinutes
    ) {
        public OrderProperties(int cancellationWindowMinutes) {
            this(cancellationWindowMinutes, 60);
        }

        public OrderProperties {
            if (undoCancelWindowMinutes <= 0) {
                undoCancelWindowMinutes = 60;
            }
        }
    }

    public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        boolean allowCredentials
    ) {}

    public record PaginationProperties(
        int defaultPage,
        int defaultPageSize,
        int maxPageSize
    ) {
        public PaginationProperties {
            if (defaultPageSize <= 0) defaultPageSize = 20;
            if (maxPageSize <= 0) maxPageSize = 100;
        }
    }

    public record ProductProperties(
        int maxPrimaryImages,
        int maxSecondaryImages
    ) {
        public ProductProperties {
            if (maxPrimaryImages <= 0) maxPrimaryImages = 1;
            if (maxSecondaryImages <= 0) maxSecondaryImages = 10;
        }

        public int maxTotalImages() {
            return maxPrimaryImages + maxSecondaryImages;
        }
    }

    public record NotificationProperties(
        ZaloProperties zalo,
        EmailProperties email
    ) {
        public NotificationProperties {
            if (zalo == null) {
                zalo = new ZaloProperties(false, "", "", "", Map.of());
            }
            if (email == null) {
                email = new EmailProperties(false, "", "HMP Sales", "");
            }
        }
    }

    public record ZaloProperties(
        boolean enabled,
        String appId,
        String secretKey,
        String accessToken,
        Map<String, String> templates
    ) {
        public ZaloProperties {
            if (templates == null) {
                templates = Map.of();
            }
        }
    }

    public record EmailProperties(
        boolean enabled,
        String fromAddress,
        String fromName,
        String sendgridApiKey
    ) {}

    public record StorageProperties(
        R2Properties r2
    ) {
        public StorageProperties {
            if (r2 == null) {
                r2 = new R2Properties(false, "", "", "", "", "");
            }
        }
    }

    public record R2Properties(
        boolean enabled,
        String accountId,
        String accessKey,
        String secretKey,
        String bucketName,
        String publicDomain
    ) {}
}
