package com.hmp.sales.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Lớp tiện ích xử lý và chuẩn hóa chuỗi tiếng Việt phục vụ cho tìm kiếm và gợi ý.
 * <p>
 * Cung cấp các chức năng:
 * <ul>
 *   <li>Chuyển đổi chuỗi tiếng Việt có dấu thành không dấu chuẩn xác (xử lý cả ký tự đặc thù như đ/Đ).</li>
 *   <li>Chuẩn hóa về chữ thường không dấu và loại bỏ khoảng trắng dư thừa.</li>
 *   <li>Tính toán khoảng cách Levenshtein (Levenshtein Distance) phục vụ tìm kiếm gần đúng (Fuzzy Search / Typo Tolerance).</li>
 * </ul>
 */
public final class VietnameseStringUtils {

    private static final Pattern COMBINING_DIACRITICAL_MARKS =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final Pattern MULTIPLE_SPACES =
            Pattern.compile("\\s+");

    private VietnameseStringUtils() {
        // Private constructor để ngăn việc khởi tạo utility class
    }

    /**
     * Chuyển đổi một chuỗi tiếng Việt có dấu thành không dấu và chuẩn hóa về chữ thường (lowercase).
     * <p>
     * Các xử lý bao gồm:
     * <ul>
     *   <li>Chuyển 'đ' / 'Đ' thành 'd'.</li>
     *   <li>Tách dấu qua {@link Normalizer.Form#NFD} và loại bỏ các dấu tổ hợp (Combining Diacritical Marks).</li>
     *   <li>Chuyển toàn bộ về chữ thường theo {@link Locale#ROOT}.</li>
     *   <li>Rút gọn nhiều khoảng trắng liên tiếp thành 1 khoảng trắng duy nhất và cắt khoảng trắng đầu/cuối.</li>
     * </ul>
     *
     * @param input chuỗi tiếng Việt đầu vào (có thể null)
     * @return chuỗi đã bỏ dấu và chuẩn hóa chữ thường, hoặc null nếu input là null
     */
    public static String toUnaccentLowerCase(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        // Xử lý riêng ký tự đ và Đ vì Normalizer NFD không tự động tách đ thành d
        String replacedD = trimmed.replace('đ', 'd').replace('Đ', 'd');

        // Tách các dấu tổ hợp NFD
        String decomposed = Normalizer.normalize(replacedD, Normalizer.Form.NFD);
        String withoutMarks = COMBINING_DIACRITICAL_MARKS.matcher(decomposed).replaceAll("");

        // Chuẩn hóa chữ thường và khoảng trắng
        String lower = withoutMarks.toLowerCase(Locale.ROOT);
        return MULTIPLE_SPACES.matcher(lower).replaceAll(" ");
    }

    /**
     * Chuyển đổi một chuỗi tiếng Việt có dấu thành không dấu nhưng giữ nguyên dạng chữ hoa/thường ban đầu.
     *
     * @param input chuỗi tiếng Việt đầu vào (có thể null)
     * @return chuỗi đã bỏ dấu, hoặc null nếu input là null
     */
    public static String toUnaccent(String input) {
        if (input == null) {
            return null;
        }
        if (input.trim().isEmpty()) {
            return "";
        }

        String replacedD = input.replace('đ', 'd').replace('Đ', 'D');
        String decomposed = Normalizer.normalize(replacedD, Normalizer.Form.NFD);
        String withoutMarks = COMBINING_DIACRITICAL_MARKS.matcher(decomposed).replaceAll("");
        return MULTIPLE_SPACES.matcher(withoutMarks.trim()).replaceAll(" ");
    }

    /**
     * Tính toán khoảng cách Levenshtein (Levenshtein Distance) giữa hai chuỗi.
     * <p>
     * Khoảng cách Levenshtein là số phép biến đổi ký tự tối thiểu (chèn, xóa, thay thế)
     * cần thiết để biến đổi chuỗi này thành chuỗi kia.
     * Thuật toán được tối ưu bộ nhớ $O(min(M, N))$ space.
     *
     * @param s1 chuỗi thứ nhất
     * @param s2 chuỗi thứ hai
     * @return khoảng cách chỉnh sửa giữa 2 chuỗi (số nguyên $\ge 0$)
     */
    public static int calculateLevenshteinDistance(CharSequence s1, CharSequence s2) {
        if (s1 == null) {
            return s2 == null ? 0 : s2.length();
        }
        if (s2 == null) {
            return s1.length();
        }

        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 == 0) {
            return len2;
        }
        if (len2 == 0) {
            return len1;
        }

        // Đảm bảo s1 luôn là chuỗi ngắn hơn để tối ưu mảng nhớ
        if (len1 > len2) {
            CharSequence tempSeq = s1;
            s1 = s2;
            s2 = tempSeq;
            int tempLen = len1;
            len1 = len2;
            len2 = tempLen;
        }

        int[] previousRow = new int[len1 + 1];
        int[] currentRow = new int[len1 + 1];

        for (int i = 0; i <= len1; i++) {
            previousRow[i] = i;
        }

        for (int j = 1; j <= len2; j++) {
            currentRow[0] = j;
            char char2 = s2.charAt(j - 1);

            for (int i = 1; i <= len1; i++) {
                char char1 = s1.charAt(i - 1);
                int cost = (char1 == char2) ? 0 : 1;

                currentRow[i] = Math.min(
                        Math.min(currentRow[i - 1] + 1,        // Chèn ký tự
                                previousRow[i] + 1),           // Xóa ký tự
                        previousRow[i - 1] + cost              // Thay thế ký tự
                );
            }

            int[] temp = previousRow;
            previousRow = currentRow;
            currentRow = temp;
        }

        return previousRow[len1];
    }
}

