package it.unical.ea_project.controller;


import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/organizer/revenue")
    public ResponseEntity<BigDecimal> getOrganizerPaidRevenue(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(paymentService.getPaidAmountForOrganizer(user.id()));
    }
}
