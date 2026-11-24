package com.example.cashly_backend.service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.cashly_backend.dto.CouponResponse;
import com.example.cashly_backend.provider.CouponProvider;

@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final List<CouponProvider> providers;
    private final Duration cacheTtl;
    private final ConcurrentHashMap<Integer, AtomicInteger> usesByCoupon = new ConcurrentHashMap<>();

    private volatile List<CouponResponse> cache = List.of();
    private volatile Instant lastLoad = Instant.EPOCH;

    CouponService(List<CouponProvider> providers,
            @Value("${cashly.coupons.cache-minutes:15}") long cacheMinutes) {
        this.providers = providers;
        this.cacheTtl = Duration.ofMinutes(cacheMinutes);
    }

    public List<CouponResponse> findAll(String category, String query, String store, boolean forceRefresh) {
        String normalizedCategory = normalize(category);
        String normalizedStore = normalize(store);
        String normalizedQuery = normalize(query);

        return load(forceRefresh).stream()
                .filter(c -> normalizedCategory.isEmpty() || normalize(c.category()).equals(normalizedCategory))
                .filter(c -> normalizedStore.isEmpty() || normalize(c.store()).contains(normalizedStore))
                .filter(c -> normalizedQuery.isEmpty() || matchesSearch(c, normalizedQuery))
                .sorted(Comparator
                        .comparing((CouponResponse c) -> !Boolean.TRUE.equals(c.featured()))
                        .thenComparing(c -> c.expiresAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Optional<CouponResponse> findById(Integer id) {
        return load(false).stream()
                .filter(c -> c.id().equals(id))
                .findFirst();
    }

    public List<String> findCategories() {
        return load(false).stream()
                .map(CouponResponse::category)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    public Optional<Integer> registerUse(Integer id) {
        if (findById(id).isEmpty()) {
            return Optional.empty();
        }
        AtomicInteger counter = usesByCoupon.computeIfAbsent(id, key -> new AtomicInteger(0));
        return Optional.of(counter.incrementAndGet());
    }

    private boolean matchesSearch(CouponResponse coupon, String query) {
        String haystack = normalize(String.join(" ",
                defaultString(coupon.store()),
                defaultString(coupon.title()),
                defaultString(coupon.description()),
                defaultString(coupon.discount()),
                defaultString(coupon.category()),
                defaultString(coupon.code())));
        return haystack.contains(query);
    }

    private List<CouponResponse> load(boolean forceRefresh) {
        if (!forceRefresh && !isStale()) {
            return cache;
        }
        synchronized (this) {
            if (!forceRefresh && !isStale()) {
                return cache;
            }
            cache = fetchFromProviders();
            lastLoad = Instant.now();
            return cache;
        }
    }

    private boolean isStale() {
        return Duration.between(lastLoad, Instant.now()).compareTo(cacheTtl) >= 0;
    }

    private List<CouponResponse> fetchFromProviders() {
        List<CouponProvider> ordered = providers.stream()
                .filter(CouponProvider::isAvailable)
                .sorted(Comparator.comparing(CouponProvider::isFallback))
                .toList();

        for (CouponProvider provider : ordered) {
            try {
                List<CouponResponse> fetched = provider.fetch();
                if (fetched != null && !fetched.isEmpty()) {
                    log.info("Feed de cupons carregado por {}", provider.getClass().getSimpleName());
                    return hydrate(fetched);
                }
            } catch (Exception e) {
                log.warn("Falha ao carregar cupons de {}: {}",
                        provider.getClass().getSimpleName(), e.getMessage());
            }
        }
        return List.of();
    }

    private List<CouponResponse> hydrate(List<CouponResponse> coupons) {
        return coupons.stream().map(coupon -> {
            AtomicInteger counter = usesByCoupon.computeIfAbsent(
                    coupon.id(),
                    key -> new AtomicInteger(initialUses(coupon)));
            return new CouponResponse(
                    coupon.id(), coupon.externalId(), coupon.store(), coupon.title(), coupon.description(),
                    coupon.code(), coupon.discount(), coupon.category(), coupon.imageUrl(), coupon.affiliateUrl(),
                    coupon.expiresAt(), counter.get(), coupon.featured(), coupon.source());
        }).toList();
    }

    private static int initialUses(CouponResponse coupon) {
        if (coupon.uses() != null && coupon.uses() > 0) {
            return coupon.uses();
        }
        return 50 + Math.abs(coupon.id() % 200);
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String noAccents = DIACRITICS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
        return noAccents.toLowerCase(Locale.ROOT).trim();
    }
}
