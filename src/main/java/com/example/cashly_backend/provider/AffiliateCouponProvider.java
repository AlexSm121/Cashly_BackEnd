package com.example.cashly_backend.provider;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.example.cashly_backend.dto.CouponResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * Provedor genérico para APIs de cupons de redes de afiliados (Lomadee, Afilio,
 * Rakuten, Awin etc.). Os nomes dos campos são normalizados para que a API da
 * rede escolhida possa ser trocada apenas pelas variáveis de ambiente.
 */
@Component
public class AffiliateCouponProvider implements CouponProvider {

    private final RestClient restClient;
    private final String apiUrl;

    AffiliateCouponProvider(
            @Value("${cashly.coupons.api.url:}") String apiUrl,
            @Value("${cashly.coupons.api.key:}") String apiKey) {
        this.apiUrl = apiUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        factory.setReadTimeout(java.time.Duration.ofSeconds(10));

        RestClient.Builder localBuilder = RestClient.builder().requestFactory(factory);
        if (StringUtils.hasText(apiKey)) {
            localBuilder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        this.restClient = localBuilder.build();
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(apiUrl);
    }

    @Override
    public List<CouponResponse> fetch() {
        JsonNode body = restClient.get()
                .uri(apiUrl)
                .retrieve()
                .body(JsonNode.class);

        JsonNode items = extractItems(body);
        List<CouponResponse> coupons = new ArrayList<>();

        for (JsonNode node : items) {
            if (node != null && node.isObject()) {
                coupons.add(toCoupon(node));
            }
        }
        return coupons;
    }

    private JsonNode extractItems(JsonNode body) {
        if (body == null || body.isNull()) {
            return JsonNodeFactory.instance.arrayNode();
        }
        if (body.isArray()) {
            return body;
        }
        for (String key : new String[] { "coupons", "data", "results", "items", "offers", "deals" }) {
            JsonNode candidate = body.get(key);
            if (candidate != null && candidate.isArray()) {
                return candidate;
            }
            if (candidate != null && candidate.isObject()) {
                JsonNode nested = extractItems(candidate);
                if (nested != null && nested.isArray() && !nested.isEmpty()) {
                    return nested;
                }
            }
        }
        return body;
    }

    private CouponResponse toCoupon(JsonNode node) {
        String externalId = firstText(node, "externalId", "id", "couponId", "offerId", "voucherId", "promotionId");
        if (!StringUtils.hasText(externalId)) {
            externalId = UUID.nameUUIDFromBytes(node.toString().getBytes(StandardCharsets.UTF_8)).toString();
        }

        String store = firstText(node, "storeName", "advertiserName", "merchantName", "store", "merchant",
                "advertiser");
        String title = firstText(node, "title", "name", "offerTitle", "headline");
        String discount = firstText(node, "discount", "discountText", "offer", "value", "savings");
        String description = firstText(node, "description", "shortDescription", "offerDescription", "details");

        if (!StringUtils.hasText(title)) {
            title = StringUtils.hasText(discount) ? discount : "Oferta em " + defaultString(store, "loja parceira");
        }

        return new CouponResponse(
                externalId.hashCode() & 0x7fffffff,
                externalId,
                defaultString(store, "Loja parceira"),
                title,
                defaultString(description, "Confira esta oferta diretamente na loja."),
                firstText(node, "couponCode", "code", "promoCode", "voucherCode", "discountCode"),
                discount,
                firstText(node, "categoryName", "category", "vertical", "segment"),
                firstText(node, "imageUrl", "logoUrl", "logo", "merchantLogo", "image"),
                firstText(node, "affiliateUrl", "trackingUrl", "deeplink", "deepLink", "clickUrl", "url", "link"),
                firstDate(node, "expiresAt", "expirationDate", "endDate", "validUntil", "expiryDate", "validTo"),
                0,
                firstBool(node, "featured", "isFeatured", "isExclusive", "exclusive"),
                "affiliate");
    }

    private static String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isObject()) {
                String nested = firstText(value, "name", "storeName", "title", "value");
                if (StringUtils.hasText(nested)) {
                    return nested;
                }
            } else if (StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return null;
    }

    private static boolean firstBool(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isBoolean()) {
                return value.booleanValue();
            }
            if (value != null && value.isValueNode() && StringUtils.hasText(value.asText())) {
                return Boolean.parseBoolean(value.asText());
            }
        }
        return false;
    }

    private static LocalDateTime firstDate(JsonNode node, String... keys) {
        String raw = firstText(node, keys);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(raw).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(raw).atStartOfDay();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(raw)).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception ignored) {
        }
        return null;
    }
}
