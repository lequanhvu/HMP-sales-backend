package com.hmp.sales.feature.delivery.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.feature.delivery.dto.DeliveryDistanceResponse;
import com.hmp.sales.feature.delivery.dto.StoreInfoResponse;
import com.hmp.sales.feature.delivery.service.DeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Controller công khai cung cấp các API kiểm tra thông tin vị trí xưởng và tính khoảng cách giao hàng.
 */
@RestController
@RequestMapping("/api/v1/delivery")
public class PublicDeliveryController {

    private final DeliveryService deliveryService;

    public PublicDeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /**
     * Lấy thông tin vị trí cơ sở của xưởng sản xuất và bán kính phục vụ tối đa.
     *
     * @return {@link ApiSuccessResponse} chứa {@link StoreInfoResponse}
     */
    @GetMapping("/store-info")
    public ResponseEntity<ApiSuccessResponse<StoreInfoResponse>> getStoreInfo() {
        StoreInfoResponse response = deliveryService.getStoreInfo();
        return ResponseEntity.ok(ApiSuccessResponse.success("Lấy thông tin cơ sở xưởng thành công", response));
    }

    /**
     * Kiểm tra khoảng cách thực tế từ xưởng đến tọa độ người nhận (vĩ độ, kinh độ).
     *
     * @param lat vĩ độ người nhận (latitude)
     * @param lng kinh độ người nhận (longitude)
     * @return {@link ApiSuccessResponse} chứa {@link DeliveryDistanceResponse}
     */
    @GetMapping("/check-distance")
    public ResponseEntity<ApiSuccessResponse<DeliveryDistanceResponse>> checkDistance(
            @RequestParam("lat") BigDecimal lat,
            @RequestParam("lng") BigDecimal lng
    ) {
        DeliveryDistanceResponse response = deliveryService.checkDistance(lat, lng);
        return ResponseEntity.ok(ApiSuccessResponse.success("Kiểm tra khoảng cách giao hàng thành công", response));
    }
}
