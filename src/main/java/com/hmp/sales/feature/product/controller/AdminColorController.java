package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.common.util.PaginationUtils;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.product.dto.ColorRequest;
import com.hmp.sales.feature.product.dto.ColorResponse;
import com.hmp.sales.feature.product.service.ColorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller dành cho Admin quản lý danh mục Màu sắc sản xuất (Color).
 * <p>
 * Cung cấp API tìm kiếm/phân trang, tạo mới, chỉnh sửa tên,
 * khóa/mở khóa trạng thái và xóa màu sắc (chặn xóa nếu đã có biến thể liên kết).
 */
@RestController
@RequestMapping("/api/v1/admin/colors")
public class AdminColorController {

    private final ColorService colorService;
    private final AppProperties appProperties;

    /**
     * Khởi tạo AdminColorController với ColorService và AppProperties.
     *
     * @param colorService  service quản lý màu sắc
     * @param appProperties cấu hình ứng dụng bao gồm phân trang từ application.yaml
     */
    @Autowired
    public AdminColorController(ColorService colorService,
                                @Autowired(required = false) AppProperties appProperties) {
        this.colorService = colorService;
        this.appProperties = appProperties;
    }

    /**
     * Khởi tạo AdminColorController đơn giản (dành cho unit test).
     *
     * @param colorService service quản lý màu sắc
     */
    public AdminColorController(ColorService colorService) {
        this(colorService, null);
    }

    /**
     * Tìm kiếm và phân trang danh sách màu sắc.
     * <p>
     * Giá trị mặc định của {@code page} và {@code size} được nạp động từ file cấu hình {@code application.yaml}.
     *
     * @param keyword từ khóa tìm theo tên (tùy chọn)
     * @param status  lọc theo trạng thái (tùy chọn)
     * @param page    chỉ số trang (tùy chọn, mặc định từ app.pagination.default-page)
     * @param size    số lượng bản ghi mỗi trang (tùy chọn, mặc định từ app.pagination.default-page-size)
     * @return danh sách màu sắc có phân trang
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageResponse<ColorResponse>>> listColors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNumber = PaginationUtils.resolvePage(page, appProperties);
        int pageSize = PaginationUtils.resolveSize(size, appProperties);
        Page<ColorResponse> result = colorService.searchColors(keyword, status, pageNumber, pageSize);
        return ResponseEntity.ok(ApiSuccessResponse.success(PageResponse.from(result)));
    }

    /**
     * Lấy thông tin màu sắc theo ID.
     *
     * @param colorId ID màu sắc cần lấy
     * @return chi tiết màu sắc
     */
    @GetMapping("/{colorId}")
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> getColorById(@PathVariable Long colorId) {
        ColorResponse response = colorService.getColorById(colorId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }

    /**
     * Tạo mới màu sắc bằng form-data kèm tệp ảnh mẫu trực tiếp (1 bước duy nhất).
     *
     * @param colorName tên màu sắc (bắt buộc, tối đa 100 ký tự)
     * @param image     tệp ảnh tải lên từ máy tính (tùy chọn)
     * @param imageUrl  đường dẫn URL ảnh trực tiếp (tùy chọn, dùng nếu không tải tệp)
     * @return thông tin màu sắc vừa tạo
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> createColorWithFile(
            @RequestParam("colorName") @NotBlank(message = "Tên màu sắc không được để trống.")
            @Size(max = 100, message = "Tên màu sắc không được vượt quá 100 ký tự.") String colorName,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "imageUrl", required = false) String imageUrl) {
        ColorResponse response = colorService.createColor(colorName, image, imageUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tạo màu sắc thành công.", response));
    }

    /**
     * Tạo mới màu sắc bằng JSON payload (dành cho client đã có sẵn URL ảnh).
     *
     * @param request payload chứa tên màu sắc
     * @return thông tin màu sắc vừa tạo
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> createColor(
            @Valid @RequestBody ColorRequest request) {
        ColorResponse response = colorService.createColor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tạo màu sắc thành công.", response));
    }

    /**
     * Cập nhật thông tin màu sắc bằng form-data kèm tệp ảnh mẫu mới (1 bước duy nhất).
     *
     * @param colorId   ID màu sắc cần cập nhật
     * @param colorName tên màu sắc mới (bắt buộc)
     * @param image     tệp ảnh mới tải lên từ máy tính (tùy chọn)
     * @param imageUrl  đường dẫn URL ảnh mới (tùy chọn)
     * @return thông tin màu sắc sau cập nhật
     */
    @PatchMapping(value = "/{colorId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> updateColorWithFile(
            @PathVariable Long colorId,
            @RequestParam("colorName") @NotBlank(message = "Tên màu sắc không được để trống.")
            @Size(max = 100, message = "Tên màu sắc không được vượt quá 100 ký tự.") String colorName,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "imageUrl", required = false) String imageUrl) {
        ColorResponse response = colorService.updateColor(colorId, colorName, image, imageUrl);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật màu sắc thành công.", response));
    }

    /**
     * Cập nhật thông tin màu sắc bằng JSON payload.
     *
     * @param colorId ID màu sắc cần cập nhật
     * @param request payload chứa thông tin mới
     * @return thông tin màu sắc sau cập nhật
     */
    @PatchMapping(value = "/{colorId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> updateColor(
            @PathVariable Long colorId,
            @Valid @RequestBody ColorRequest request) {
        ColorResponse response = colorService.updateColor(colorId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật màu sắc thành công.", response));
    }

    /**
     * Khóa màu sắc (chuyển sang LOCKED).
     *
     * @param colorId ID màu sắc cần khóa
     * @return thông tin màu sắc sau khi khóa
     */
    @PostMapping("/{colorId}/lock")
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> lockColor(@PathVariable Long colorId) {
        ColorResponse response = colorService.lockColor(colorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Khóa màu sắc thành công.", response));
    }

    /**
     * Mở khóa màu sắc (chuyển về ACTIVE).
     *
     * @param colorId ID màu sắc cần mở khóa
     * @return thông tin màu sắc sau khi mở khóa
     */
    @PostMapping("/{colorId}/unlock")
    public ResponseEntity<ApiSuccessResponse<ColorResponse>> unlockColor(@PathVariable Long colorId) {
        ColorResponse response = colorService.unlockColor(colorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Mở khóa màu sắc thành công.", response));
    }

    /**
     * Xóa mềm màu sắc (đánh dấu is_deleted = true).
     * <p>
     * Chặn xóa nếu màu sắc đang được liên kết trong bất kỳ biến thể sản phẩm nào.
     *
     * @param colorId ID màu sắc cần xóa
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/{colorId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteColor(@PathVariable Long colorId) {
        colorService.deleteColor(colorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa màu sắc thành công.", null));
    }
}
