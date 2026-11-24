package com.example.cashly_backend.provider;

import java.util.List;

import com.example.cashly_backend.dto.CouponResponse;

public interface CouponProvider {

    List<CouponResponse> fetch();

    default boolean isAvailable() {
        return true;
    }

    /**
     * Provedores de fallback (demo) só são usados quando a API externa falha ou
     * não está configurada.
     */
    default boolean isFallback() {
        return false;
    }
}
