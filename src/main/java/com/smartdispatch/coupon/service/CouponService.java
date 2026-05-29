package com.smartdispatch.coupon.service;

import com.smartdispatch.coupon.entity.Coupon;
import com.smartdispatch.coupon.repository.CouponRepository;
import com.smartdispatch.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;

    // ═══════════════════════════════════════════
    // Validate & Apply Coupon
    // ═══════════════════════════════════════════
    @Transactional
    public CouponResult applyCoupon(String code, Double orderAmount) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new BadRequestException("Invalid coupon code: " + code));

        if (!coupon.isValid()) {
            throw new BadRequestException("Coupon has expired or reached its usage limit");
        }

        if (coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
            throw new BadRequestException("Minimum order amount ₹" + coupon.getMinOrderAmount() + " required");
        }

        double discount;
        if ("PERCENTAGE".equals(coupon.getDiscountType())) {
            discount = orderAmount * (coupon.getDiscountValue() / 100);
            if (coupon.getMaxDiscount() != null) {
                discount = Math.min(discount, coupon.getMaxDiscount());
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        discount = Math.min(discount, orderAmount); // Cannot exceed order amount
        double finalAmount = orderAmount - discount;

        // Increment usage
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        log.info("Coupon {} applied. Discount: ₹{}, Final: ₹{}", code, discount, finalAmount);

        return new CouponResult(coupon.getCode(), discount, Math.round(finalAmount * 100.0) / 100.0);
    }

    // ═══════════════════════════════════════════
    // Get Available Coupons
    // ═══════════════════════════════════════════
    public List<Coupon> getActiveCoupons() {
        return couponRepository.findByActiveTrue().stream()
                .filter(Coupon::isValid)
                .toList();
    }

    // ═══════════════════════════════════════════
    // Create Coupon (Admin)
    // ═══════════════════════════════════════════
    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        coupon.setCode(coupon.getCode().toUpperCase());
        if (couponRepository.findByCode(coupon.getCode()).isPresent()) {
            throw new BadRequestException("Coupon code already exists");
        }
        return couponRepository.save(coupon);
    }

    public record CouponResult(String code, double discount, double finalAmount) {}
}
