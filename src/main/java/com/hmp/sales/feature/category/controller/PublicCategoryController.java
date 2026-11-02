package com.hmp.sales.feature.category.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.util.PaginationUtils;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.category.dto.CategoryProductsResponse;
import com.hmp.sales.feature.category.dto.CategoryTreeResponse;
import com.hmp.sales.feature.category.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller công khai phục vụ tra cứu danh mục sản phẩm (Category).
 * <p>
 * Cho phép khách vãng lai và khách hàng xem cấu trúc cây danh mục
 * và danh sách sản phẩm thuộc danh mục mà không cần đăng nhập.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class PublicCategoryController {

    private final CategoryService categoryService;
    private final AppProperties appProperties;

    /**
     * Khởi tạo PublicCategoryController với CategoryService và AppProperties.
     *
     * @param categoryService service quản lý danh mục sản phẩm
     * @param appProperties  cấu hình hệ thống bao gồm phân trang
     */
    @Autowired
    public PublicCategoryController(CategoryService categoryService,
                                    @Autowired(required = false) AppProperties appProperties) {
        this.categoryService = categoryService;
        this.appProperties = appProperties;
    }

    /**
     * Khởi tạo PublicCategoryController đơn giản (dành cho unit test).
     *
     * @param categoryService service quản lý danh mục sản phẩm
     */
    public PublicCategoryController(CategoryService categoryService) {
        this(categoryService, null);
    }

    /**
     * Lấy toàn bộ cây danh mục sản phẩm theo cấu trúc phân cấp n-cấp.
     * <p>
     * Công khai cho mọi đối tượng người dùng.
     *
     * @return danh sách các danh mục cấp gốc, mỗi danh mục chứa danh sách con lồng nhau
     */
    @GetMapping("/tree")
    public ResponseEntity<ApiSuccessResponse<List<CategoryTreeResponse>>> getCategoryTree() {
        List<CategoryTreeResponse> tree = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiSuccessResponse.success(tree));
    }

    /**
     * Lấy danh sách sản phẩm đang hoạt động thuộc một danh mục cụ thể có phân trang.
     * <p>
     * Giá sản phẩm sẽ được ẩn nếu người gọi chưa đăng nhập hoặc không có quyền xem giá.
     * Tham số page và size được giải quyết linh hoạt qua cấu hình hệ thống.
     *
     * @param categoryId ID danh mục cần tra cứu
     * @param page       chỉ số trang (tùy chọn, 0-indexed)
     * @param size       kích thước trang (tùy chọn)
     * @return danh sách sản phẩm thuộc danh mục dạng phân trang (result, meta)
     */
    @GetMapping("/{categoryId}/products")
    public ResponseEntity<ApiSuccessResponse<CategoryProductsResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageIndex = PaginationUtils.resolvePage(page, appProperties);
        int pageSize = PaginationUtils.resolveSize(size, appProperties);
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        CategoryProductsResponse response = categoryService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }
}
