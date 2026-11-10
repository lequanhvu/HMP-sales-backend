package com.hmp.sales.feature.product.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.product.dto.ColorRequest;
import com.hmp.sales.feature.product.dto.ColorResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Giao diện nghiệp vụ quản lý màu sắc tôn sản xuất (Color) cho Admin.
 * Cung cấp CRUD đầy đủ, tìm kiếm/phân trang, khóa/mở khóa,
 * và lấy danh sách dropdown công khai.
 */
public interface ColorService {

    /**
     * Tìm kiếm màu sắc với phân trang (dành cho Admin).
     *
     * @param keyword  từ khóa tìm theo tên (null để lấy tất cả)
     * @param status   lọc theo trạng thái (null để lấy tất cả)
     * @param page     chỉ số trang
     * @param size     số bản ghi mỗi trang
     * @return trang màu sắc phù hợp
     */
    Page<ColorResponse> searchColors(String keyword, String status, int page, int size);

    /**
     * Lấy thông tin màu sắc theo ID.
     *
     * @param colorId ID màu sắc
     * @return thông tin màu sắc
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    ColorResponse getColorById(Long colorId);

    /**
     * Tạo mới màu sắc.
     *
     * @param request payload chứa tên màu sắc
     * @return thông tin màu sắc vừa tạo
     * @throws BusinessRuleException nếu tên đã tồn tại
     */
    ColorResponse createColor(ColorRequest request);

    /**
     * Tạo mới màu sắc kèm tệp ảnh mẫu (tùy chọn) trong 1 bước duy nhất.
     * <p>
     * Nếu có truyền tệp ảnh {@code imageFile}, hệ thống sẽ tự động tải lên dịch vụ lưu trữ
     * đám mây (Cloudflare R2) và lưu URL công khai vào trường {@code imageUrl}.
     *
     * @param colorName tên màu sắc (bắt buộc)
     * @param imageFile tệp ảnh mẫu tải lên từ máy tính (tùy chọn)
     * @param imageUrl  đường dẫn URL ảnh trực tiếp (tùy chọn, dùng nếu không truyền tệp)
     * @return thông tin màu sắc vừa tạo
     * @throws BusinessRuleException nếu tên màu đã tồn tại hoặc tệp ảnh không hợp lệ
     */
    ColorResponse createColor(String colorName, MultipartFile imageFile, String imageUrl);

    /**
     * Cập nhật tên màu sắc.
     *
     * @param colorId ID màu sắc cần cập nhật
     * @param request payload chứa tên mới
     * @return thông tin màu sắc sau khi cập nhật
     * @throws ResourceNotFoundException nếu không tìm thấy
     * @throws BusinessRuleException    nếu tên mới đã tồn tại
     */
    ColorResponse updateColor(Long colorId, ColorRequest request);

    /**
     * Cập nhật thông tin màu sắc kèm tệp ảnh mẫu mới (tùy chọn) trong 1 bước duy nhất.
     *
     * @param colorId   ID màu sắc cần cập nhật
     * @param colorName tên màu sắc mới
     * @param imageFile tệp ảnh mẫu mới tải lên từ máy tính (tùy chọn)
     * @param imageUrl  đường dẫn URL ảnh mới (tùy chọn)
     * @return thông tin màu sắc sau cập nhật
     * @throws ResourceNotFoundException nếu không tìm thấy
     * @throws BusinessRuleException    nếu tên mới đã tồn tại hoặc tệp ảnh không hợp lệ
     */
    ColorResponse updateColor(Long colorId, String colorName, MultipartFile imageFile, String imageUrl);

    /**
     * Khóa màu sắc.
     *
     * @param colorId ID màu sắc cần khóa
     * @return thông tin màu sắc sau khi khóa
     */
    ColorResponse lockColor(Long colorId);

    /**
     * Mở khóa màu sắc.
     *
     * @param colorId ID màu sắc cần mở khóa
     * @return thông tin màu sắc sau khi mở khóa
     */
    ColorResponse unlockColor(Long colorId);

    /**
     * Xóa mềm màu sắc. Chặn xóa nếu còn biến thể sản phẩm liên kết.
     *
     * @param colorId ID màu sắc cần xóa
     * @throws ResourceNotFoundException nếu không tìm thấy
     * @throws BusinessRuleException    nếu còn biến thể liên kết
     */
    void deleteColor(Long colorId);

    /**
     * Lấy danh sách màu sắc đang ACTIVE cho dropdown.
     *
     * @return danh sách màu sắc đang hoạt động
     */
    List<ColorResponse> getActiveColors();
}
