package com.smartdispatch.payment.provider;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Wallet payment provider.
 * Deducts from user's wallet balance.
 */
@Component("walletProvider")
@RequiredArgsConstructor
@Slf4j
public class WalletPaymentProvider implements PaymentProvider {

    private final UserRepository userRepository;

    @Override
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        User user = userRepository.findById(Long.valueOf(customerId))
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getWalletBalance() < amount) {
            return new PaymentResult(false, null, null, "Insufficient wallet balance");
        }

        user.setWalletBalance(user.getWalletBalance() - amount);
        userRepository.save(user);

        String txnId = "WLT-" + System.currentTimeMillis();
        log.info("[WALLET] Payment of ₹{} processed. TxnID: {}", amount, txnId);

        return new PaymentResult(true, txnId, null, null);
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        log.info("[WALLET] Refund of ₹{} processed for txn: {}", amount, transactionId);
        return new PaymentResult(true, "REF-" + transactionId, null, null);
    }

    @Override
    public String getProviderName() { return "WALLET"; }
}
