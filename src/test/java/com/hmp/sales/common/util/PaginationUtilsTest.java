package com.hmp.sales.common.util;

import com.hmp.sales.config.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaginationUtils Unit Tests")
class PaginationUtilsTest {

    private final AppProperties customProps = new AppProperties(
            null, null, null, null,
            new AppProperties.PaginationProperties(2, 15, 50),
            null
    );

    @Test
    @DisplayName("Constructor riêng tư ném UnsupportedOperationException")
    void privateConstructor_throwsException() throws Exception {
        Constructor<PaginationUtils> constructor = PaginationUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, ex.getCause());
    }

    @Test
    @DisplayName("resolvePage với tham số cụ thể trả về giá trị đó (không âm)")
    void resolvePage_withExplicitValue_returnsValue() {
        assertEquals(5, PaginationUtils.resolvePage(5, customProps));
        assertEquals(0, PaginationUtils.resolvePage(-3, customProps));
    }

    @Test
    @DisplayName("resolvePage với null lấy default từ AppProperties")
    void resolvePage_withNull_returnsDefaultFromProperties() {
        assertEquals(2, PaginationUtils.resolvePage(null, customProps));
    }

    @Test
    @DisplayName("resolvePage với null và appProperties null trả về 0")
    void resolvePage_withNullProps_returnsZero() {
        assertEquals(0, PaginationUtils.resolvePage(null, null));
    }

    @Test
    @DisplayName("resolveSize với tham số cụ thể trả về giá trị đó (có chặn maxPageSize)")
    void resolveSize_withExplicitValue_clampsToMax() {
        assertEquals(30, PaginationUtils.resolveSize(30, customProps));
        assertEquals(50, PaginationUtils.resolveSize(80, customProps));
    }

    @Test
    @DisplayName("resolveSize với null hoặc <= 0 lấy defaultPageSize từ AppProperties")
    void resolveSize_withNullOrZero_returnsDefaultFromProperties() {
        assertEquals(15, PaginationUtils.resolveSize(null, customProps));
        assertEquals(15, PaginationUtils.resolveSize(0, customProps));
        assertEquals(15, PaginationUtils.resolveSize(-5, customProps));
    }

    @Test
    @DisplayName("resolveSize với null và appProperties null trả về 20")
    void resolveSize_withNullProps_returnsFallback() {
        assertEquals(20, PaginationUtils.resolveSize(null, null));
        assertEquals(25, PaginationUtils.resolveSize(25, null));
        assertEquals(100, PaginationUtils.resolveSize(150, null));
    }
}

