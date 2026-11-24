package com.example.cashly_backend.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.cashly_backend.dto.CouponResponse;
import com.example.cashly_backend.service.CouponService;

@RestController
public class CouponController {

    private final CouponService couponService;

    CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<CouponResponse>> getCoupons(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String store,
            @RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(couponService.findAll(category, q, store, refresh));
    }

    @GetMapping("/coupons/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(couponService.findCategories());
    }

    @GetMapping("/coupons/{id}")
    public ResponseEntity<CouponResponse> getCouponById(@PathVariable Integer id) {
        return couponService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/coupons/{id}/use")
    public ResponseEntity<Map<String, Integer>> registerUse(@PathVariable Integer id) {
        Optional<Integer> uses = couponService.registerUse(id);
        return uses
                .map(count -> ResponseEntity.ok(Map.of("uses", count)))
                .orElse(ResponseEntity.notFound().build());
    }
}
