package com.smartdispatch.payment.controller;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.order.entity.Order;
import com.smartdispatch.order.enums.OrderStatus;
import com.smartdispatch.order.repository.OrderRepository;
import com.smartdispatch.payment.entity.Payment;
import com.smartdispatch.payment.enums.PaymentStatus;
import com.smartdispatch.payment.provider.PayUPaymentProvider;
import com.smartdispatch.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/api/v1/payments/payu")
@RequiredArgsConstructor
@Slf4j
public class PayUController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PayUPaymentProvider payUPaymentProvider;

    @GetMapping(value = "/checkout", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String checkOut(@RequestParam("txnid") String txnid) {
        Payment payment = paymentRepository.findByProviderTransactionId(txnid)
                .orElseThrow(() -> new IllegalArgumentException("Payment session not found"));

        User customer = userRepository.findById(payment.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        String amountFormatted = String.format(java.util.Locale.US, "%.2f", payment.getAmount());
        String productInfo = "Smart Dispatch Order " + payment.getOrderId();
        String firstName = customer.getFirstName() != null ? customer.getFirstName() : "Customer";
        if (firstName.trim().isEmpty()) firstName = "Customer";
        String email = customer.getEmail();
        String phone = customer.getPhoneNo() != null ? customer.getPhoneNo() : "9999999999";

        String key = payUPaymentProvider.getKey();
        String salt = payUPaymentProvider.getSalt();
        String hashString = key + "|" + txnid + "|" + amountFormatted + "|" + productInfo + "|" + firstName + "|" + email + "|||||||||||" + salt;
        String hash = payUPaymentProvider.hashCal("SHA-512", hashString);

        String surl = payUPaymentProvider.getBackendUrl() + "/api/v1/payments/payu/success";
        String furl = payUPaymentProvider.getBackendUrl() + "/api/v1/payments/payu/failure";

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Redirecting to PayU...</title>\n" +
                "</head>\n" +
                "<body onload=\"document.forms['payuForm'].submit();\">\n" +
                "    <form name=\"payuForm\" action=\"https://test.payu.in/_payment\" method=\"post\">\n" +
                "        <input type=\"hidden\" name=\"key\" value=\"" + key + "\" />\n" +
                "        <input type=\"hidden\" name=\"txnid\" value=\"" + txnid + "\" />\n" +
                "        <input type=\"hidden\" name=\"amount\" value=\"" + amountFormatted + "\" />\n" +
                "        <input type=\"hidden\" name=\"productinfo\" value=\"" + productInfo + "\" />\n" +
                "        <input type=\"hidden\" name=\"firstname\" value=\"" + firstName + "\" />\n" +
                "        <input type=\"hidden\" name=\"email\" value=\"" + email + "\" />\n" +
                "        <input type=\"hidden\" name=\"phone\" value=\"" + phone + "\" />\n" +
                "        <input type=\"hidden\" name=\"surl\" value=\"" + surl + "\" />\n" +
                "        <input type=\"hidden\" name=\"furl\" value=\"" + furl + "\" />\n" +
                "        <input type=\"hidden\" name=\"hash\" value=\"" + hash + "\" />\n" +
                "        <input type=\"hidden\" name=\"service_provider\" value=\"payu_paisa\" />\n" +
                "    </form>\n" +
                "</body>\n" +
                "</html>";
    }

    @PostMapping(value = "/success", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String paymentSuccess(@RequestParam Map<String, String> params) {
        log.info("[PAYU SUCCESS] Callback received: {}", params);
        String txnid = params.get("txnid");

        paymentRepository.findByProviderTransactionId(txnid).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            payment.setGatewayPaymentId(params.get("mihpayid"));
            paymentRepository.save(payment);

            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                log.info("[PAYU SUCCESS] Order {} confirmed.", order.getId());
            });
        });

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Payment Successful</title>\n" +
                "    <style>\n" +
                "        body { font-family: sans-serif; text-align: center; padding: 50px; background-color: #F8FAFC; }\n" +
                "        .card { background: white; padding: 40px; border-radius: 20px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); display: inline-block; }\n" +
                "        h1 { color: #10B981; }\n" +
                "        p { color: #64748B; font-size: 16px; }\n" +
                "        button { background: #0F172A; color: white; border: none; padding: 12px 24px; border-radius: 10px; font-weight: bold; cursor: pointer; margin-top: 20px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"card\">\n" +
                "        <h1>Payment Successful!</h1>\n" +
                "        <p>Your booking payment has been verified successfully.</p>\n" +
                "        <button onclick=\"window.close();\">Go Back to App</button>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    @PostMapping(value = "/failure", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String paymentFailure(@RequestParam Map<String, String> params) {
        log.warn("[PAYU FAILURE] Callback received: {}", params);
        String txnid = params.get("txnid");

        paymentRepository.findByProviderTransactionId(txnid).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(params.get("unmappedstatus"));
            paymentRepository.save(payment);

            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                orderRepository.save(order);
                log.warn("[PAYU FAILURE] Order {} marked as failed.", order.getId());
            });
        });

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Payment Failed</title>\n" +
                "    <style>\n" +
                "        body { font-family: sans-serif; text-align: center; padding: 50px; background-color: #F8FAFC; }\n" +
                "        .card { background: white; padding: 40px; border-radius: 20px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); display: inline-block; }\n" +
                "        h1 { color: #EF4444; }\n" +
                "        p { color: #64748B; font-size: 16px; }\n" +
                "        button { background: #0F172A; color: white; border: none; padding: 12px 24px; border-radius: 10px; font-weight: bold; cursor: pointer; margin-top: 20px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"card\">\n" +
                "        <h1>Payment Failed</h1>\n" +
                "        <p>There was an issue processing your transaction. Please try again.</p>\n" +
                "        <button onclick=\"window.close();\">Go Back to App</button>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}
