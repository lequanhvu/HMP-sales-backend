package com.hmp.sales.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditValueUtils Unit Tests")
class AuditValueUtilsTest {

    @Test
    @DisplayName("toJson - Giữ nguyên thứ tự chèn và loại bỏ các khóa nhạy cảm")
    void toJson_filtersSensitiveKeys() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("productName", "Tôn sóng");
        map.put("price", 100000L);
        map.put("active", true);
        map.put("password", "supersecret");
        map.put("jwtToken", "eyJ...");
        map.put("userOtp", "123456");
        map.put("nullField", null);

        String json = AuditValueUtils.toJson(map);
        assertThat(json).contains("\"productName\":\"Tôn sóng\"");
        assertThat(json).contains("\"price\":100000");
        assertThat(json).contains("\"active\":true");
        assertThat(json).doesNotContain("supersecret");
        assertThat(json).doesNotContain("eyJ");
        assertThat(json).doesNotContain("123456");
        assertThat(json).doesNotContain("nullField");
    }

    @Test
    @DisplayName("toJson - Hỗ trợ serialize Collection/List thành JSON array")
    void toJson_supportsIterableValues() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("permissions", java.util.List.of("ORDER_CREATE", "PRODUCT_VIEW"));
        map.put("role", "ADMIN");

        String json = AuditValueUtils.toJson(map);
        assertThat(json).isEqualTo("{\"permissions\":[\"ORDER_CREATE\",\"PRODUCT_VIEW\"],\"role\":\"ADMIN\"}");
    }

    @Test
    @DisplayName("toJsonArray - Dựng chuỗi JSON array từ danh sách")
    void toJsonArray_success() {
        assertThat(AuditValueUtils.toJsonArray(null)).isEqualTo("[]");
        assertThat(AuditValueUtils.toJsonArray(java.util.List.of())).isEqualTo("[]");
        assertThat(AuditValueUtils.toJsonArray(java.util.List.of("A", "B", 123, true)))
                .isEqualTo("[\"A\",\"B\",123,true]");
    }

    @Test
    @DisplayName("toJson - Trả về {} khi map rỗng hoặc null")
    void toJson_nullOrEmpty_returnsEmptyBraces() {
        assertThat(AuditValueUtils.toJson(null)).isEqualTo("{}");
        assertThat(AuditValueUtils.toJson(Map.of())).isEqualTo("{}");
    }

    @Test
    @DisplayName("maskPhone - Che đúng các ký tự giữa")
    void maskPhone_success() {
        assertThat(AuditValueUtils.maskPhone(null)).isNull();
        assertThat(AuditValueUtils.maskPhone("0987654321")).isEqualTo("09*****321");
        assertThat(AuditValueUtils.maskPhone("123")).isEqualTo("***");
    }

    @Test
    @DisplayName("maskEmail - Che tên người dùng, giữ chữ đầu và domain")
    void maskEmail_success() {
        assertThat(AuditValueUtils.maskEmail(null)).isNull();
        assertThat(AuditValueUtils.maskEmail("customer@example.com")).isEqualTo("c***@example.com");
        assertThat(AuditValueUtils.maskEmail("invalid-email")).isEqualTo("***");
    }

    @Test
    @DisplayName("truncate - Cắt ngắn chuỗi vượt quá độ dài tối đa")
    void truncate_success() {
        assertThat(AuditValueUtils.truncate(null, 10)).isNull();
        assertThat(AuditValueUtils.truncate("short", 10)).isEqualTo("short");
        assertThat(AuditValueUtils.truncate("123456789012345", 10)).isEqualTo("1234567...");
    }
}

