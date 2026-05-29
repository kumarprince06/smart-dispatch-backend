package com.smartdispatch.coupon.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.coupon.entity.Coupon;
import com.smartdispatch.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<CouponService.CouponResult>> applyCoupon(
            @RequestParam String code,
            @RequestParam Double orderAmount
    ) {
        CouponService.CouponResult result = couponService.applyCoupon(code, orderAmount);
        return ResponseEntity.ok(ApiResponse.<CouponService.CouponResult>builder()
                .success(true).message("Coupon applied! Discount: ₹" + result.discount())
                .status(200).data(result).build());
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Coupon>>> getAvailableCoupons() {
        List<Coupon> coupons = couponService.getActiveCoupons();
        return ResponseEntity.ok(ApiResponse.<List<Coupon>>builder()
                .success(true).message("Available coupons").status(200).data(coupons).build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Coupon>> createCoupon(@RequestBody Coupon coupon) {
        Coupon created = couponService.createCoupon(coupon);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Coupon>builder()
                .success(true).message("Coupon created").status(201).data(created).build());
    }
}
