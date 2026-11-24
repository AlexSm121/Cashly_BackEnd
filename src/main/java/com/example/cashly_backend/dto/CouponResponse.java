package com.example.cashly_backend.dto;

import java.time.LocalDateTime;

public record CouponResponse(
        Integer id,
        String externalId,
        String store,
        String title,
        String description,
        String code,
        String discount,
        String category,
        String imageUrl,
        String affiliateUrl,
        LocalDateTime expiresAt,
        Integer uses,
        Boolean featured,
        String source) {
}
