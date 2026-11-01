package com.hmp.sales.common.exception;

import com.hmp.sales.common.response.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleResourceNotFound returns 404 with error message")
    void handleResourceNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Sản phẩm", 999L);
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().statusCode());
        assertTrue(response.getBody().message().contains("Không tìm thấy Sản phẩm"));
    }

    @Test
    @DisplayName("handleInvalidOrderState returns 409 Conflict")
    void handleInvalidOrderState_returns409() {
        InvalidOrderStateException ex = new InvalidOrderStateException("PENDING", "COMPLETED");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleInvalidOrderState(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().statusCode());
        assertTrue(response.getBody().message().contains("Không thể chuyển trạng thái"));
    }

    @Test
    @DisplayName("handleUnauthorizedOperation returns 403 Forbidden")
    void handleUnauthorizedOperation_returns403() {
        UnauthorizedOperationException ex = new UnauthorizedOperationException("Không được phép cập nhật tài nguyên này.");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleUnauthorizedOperation(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().statusCode());
    }

    @Test
    @DisplayName("handleOtpVerification returns 400 Bad Request")
    void handleOtpVerification_returns400() {
        OtpVerificationException ex = new OtpVerificationException("Mã OTP đã hết hạn.");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleOtpVerification(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().statusCode());
        assertEquals("Mã OTP đã hết hạn.", response.getBody().message());
    }

    @Test
    @DisplayName("handleBusinessRule returns 400 Bad Request")
    void handleBusinessRule_returns400() {
        BusinessRuleException ex = new BusinessRuleException("Số lượng phải lớn hơn 0.");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBusinessRule(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().statusCode());
        assertEquals("Số lượng phải lớn hơn 0.", response.getBody().message());
    }

    @Test
    @DisplayName("handleAccessDenied returns 403 Forbidden with standard Vietnamese message")
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().statusCode());
        assertEquals("Bạn không có quyền thực hiện hành động này.", response.getBody().message());
    }

    @Test
    @DisplayName("handleAuthentication returns 401 Unauthorized with standard Vietnamese message")
    void handleAuthentication_returns401() {
        AuthenticationException ex = new AuthenticationException("Bad credentials") {};
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleAuthentication(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().statusCode());
        assertEquals("Yêu cầu chưa được xác thực hoặc thông tin xác thực không hợp lệ.", response.getBody().message());
    }

    @Test
    @DisplayName("handleUnexpectedException returns 500 without leaking details")
    void handleUnexpectedException_returns500() {
        RuntimeException ex = new RuntimeException("Sensitive database connection details");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleUnexpectedException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().statusCode());
        assertFalse(response.getBody().message().contains("Sensitive"));
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatch returns 404 for userId ID mismatch")
    void handleMethodArgumentTypeMismatch_returns404_forUserId() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "userId", null, null);
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().statusCode());
        assertEquals("Không tìm thấy người dùng ID: abc", response.getBody().message());
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatch returns 404 for categoryId ID mismatch")
    void handleMethodArgumentTypeMismatch_returns404_forCategoryId() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "categoryId", null, null);
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().statusCode());
        assertEquals("Không tìm thấy danh mục ID: abc", response.getBody().message());
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatch returns 404 for productId ID mismatch")
    void handleMethodArgumentTypeMismatch_returns404_forProductId() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "productId", null, null);
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().statusCode());
        assertEquals("Không tìm thấy sản phẩm ID: abc", response.getBody().message());
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatch returns 400 for non-ID param mismatch")
    void handleMethodArgumentTypeMismatch_returns400_forNonIdParam() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "xyz", int.class, "page", null, null);
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().statusCode());
        assertEquals("Giá trị tham số 'page' không hợp lệ: xyz", response.getBody().message());
    }
}
