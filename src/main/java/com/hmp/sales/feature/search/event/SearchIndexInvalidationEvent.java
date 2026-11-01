package com.hmp.sales.feature.search.event;

import com.hmp.sales.feature.search.dto.SearchResourceType;

/**
 * Sự kiện Spring Application Event thông báo đồng bộ lại chỉ mục tìm kiếm khi có thay đổi dữ liệu.
 *
 * @param type      loại tài nguyên (PRODUCT, CATEGORY, BRAND)
 * @param targetId  ID tài nguyên bị thay đổi
 * @param operation loại thao tác nghiệp vụ (UPSERT, DELETE)
 */
public record SearchIndexInvalidationEvent(
        SearchResourceType type,
        Long targetId,
        Operation operation
) {
    /**
     * Loại thao tác làm thay đổi dữ liệu.
     */
    public enum Operation {
        /**
         * Tạo mới hoặc cập nhật thông tin tài nguyên.
         */
        UPSERT,

        /**
         * Xóa mềm hoặc ẩn tài nguyên khỏi danh sách tìm kiếm.
         */
        DELETE,

        /**
         * Nạp lại toàn bộ chỉ mục từ đầu.
         */
        RELOAD_ALL
    }
}

