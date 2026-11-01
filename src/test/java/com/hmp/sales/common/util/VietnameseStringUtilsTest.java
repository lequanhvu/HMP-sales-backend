package com.hmp.sales.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VietnameseStringUtils Unit Tests")
class VietnameseStringUtilsTest {

    @Nested
    @DisplayName("Tests chuyển đổi toUnaccentLowerCase")
    class ToUnaccentLowerCaseTests {

        @Test
        @DisplayName("Xử lý chuỗi null hoặc rỗng")
        void shouldHandleNullAndEmpty() {
            assertThat(VietnameseStringUtils.toUnaccentLowerCase(null)).isNull();
            assertThat(VietnameseStringUtils.toUnaccentLowerCase("")).isEmpty();
            assertThat(VietnameseStringUtils.toUnaccentLowerCase("   ")).isEmpty();
        }

        @ParameterizedTest(name = "Chuyển \"{0}\" -> \"{1}\"")
        @CsvSource({
                "'Tôn sóng lợp mái', 'ton song lop mai'",
                "'TÔN SÓNG LỢP MÁI', 'ton song lop mai'",
                "'Đông Á', 'dong a'",
                "'đông á', 'dong a'",
                "'ĐỒNG NAI', 'dong nai'",
                "'Hoa Sen', 'hoa sen'",
                "'Nam Kim', 'nam kim'",
                "'Sườn, xối, nóc, máng', 'suon, xoi, noc, mang'",
                "'   Tôn   lạnh   màu   ', 'ton lanh mau'",
                "'Ốc vít 5 phân', 'oc vit 5 phan'",
                "'Lợp mái', 'lop mai'",
                "'Lọp mái', 'lop mai'"
        })
        @DisplayName("Chuẩn hóa các cụm từ tiếng Việt thực tế trong ngành tôn thép")
        void shouldNormalizeVietnamesePhrases(String input, String expected) {
            assertThat(VietnameseStringUtils.toUnaccentLowerCase(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Cả 'lợp' và 'lọp' (gõ nhầm o thay vì ơ) đều quy về 'lop'")
        void shouldNormalizeLopVariants() {
            String original = VietnameseStringUtils.toUnaccentLowerCase("Tôn sóng lợp mái");
            String typoSearch = VietnameseStringUtils.toUnaccentLowerCase("lọp");

            assertThat(original).contains(typoSearch);
        }
    }

    @Nested
    @DisplayName("Tests chuyển đổi toUnaccent (giữ nguyên hoa thường)")
    class ToUnaccentTests {

        @Test
        @DisplayName("Giữ nguyên chữ hoa và chữ thường")
        void shouldPreserveCase() {
            assertThat(VietnameseStringUtils.toUnaccent(null)).isNull();
            assertThat(VietnameseStringUtils.toUnaccent("")).isEmpty();
            assertThat(VietnameseStringUtils.toUnaccent("Đông Á")).isEqualTo("Dong A");
            assertThat(VietnameseStringUtils.toUnaccent("TÔN SÓNG")).isEqualTo("TON SONG");
        }
    }

    @Nested
    @DisplayName("Tests tính khoảng cách Levenshtein (Levenshtein Distance)")
    class LevenshteinDistanceTests {

        @Test
        @DisplayName("Hai chuỗi giống hệt nhau có khoảng cách bằng 0")
        void shouldReturnZeroForIdenticalStrings() {
            assertThat(VietnameseStringUtils.calculateLevenshteinDistance("ton song", "ton song")).isZero();
            assertThat(VietnameseStringUtils.calculateLevenshteinDistance("", "")).isZero();
            assertThat(VietnameseStringUtils.calculateLevenshteinDistance(null, null)).isZero();
        }

        @Test
        @DisplayName("Một chuỗi rỗng thì khoảng cách bằng độ dài chuỗi kia")
        void shouldReturnLengthWhenOneIsEmpty() {
            assertThat(VietnameseStringUtils.calculateLevenshteinDistance("abc", "")).isEqualTo(3);
            assertThat(VietnameseStringUtils.calculateLevenshteinDistance("", "hello")).isEqualTo(5);
            assertThat(VietnameseStringUtils.calculateLevenshteinDistance("abc", null)).isEqualTo(3);
        }

        @ParameterizedTest(name = "Khoảng cách giữa \"{0}\" và \"{1}\" là {2}")
        @CsvSource({
                "'ton sog', 'ton song', 1",       // Thiếu 1 chữ n
                "'hoa senn', 'hoa sen', 1",       // Thừa 1 chữ n
                "'ton son', 'ton song', 1",       // Thiếu 1 chữ g
                "'ton song', 'tan sang', 2",      // Thay thế 2 nguyên âm
                "'xoi mang', 'xoi man', 1",       // Thiếu 1 ký tự
                "'hoa sen', 'hoa dan', 2"         // Thay thế 2 ký tự
        })
        @DisplayName("Tính khoảng cách cho các lỗi gõ sai / gõ trượt phím phổ biến")
        void shouldCalculateCorrectDistanceForTypos(String s1, String s2, int expectedDistance) {
            assertThat(VietnameseStringUtils.calculateLevenshteinDistance(s1, s2)).isEqualTo(expectedDistance);
        }
    }
}

