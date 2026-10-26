package it.unical.ea_project.controller.payment;

import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.payment.PaymentService;
import it.unical.ea_project.service.payment.MockCheckoutService;
import it.unical.ea_project.service.payment.OrganizerStatisticsService;
import it.unical.ea_project.dto.payment.OrganizerStatisticsDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.math.BigDecimal;
import it.unical.ea_project.dto.payment.OrganizerFinanceDTO;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final MockCheckoutService checkout;
    private final OrganizerStatisticsService statistics;

    public PaymentController(PaymentService paymentService, MockCheckoutService checkout,
            OrganizerStatisticsService statistics) {
        this.checkout = checkout;
        this.statistics = statistics;
        this.paymentService = paymentService;
    }

    private void requireOrganizer(AuthUser user) {
        if (!"ORGANIZER".equals(user.role()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accesso riservato agli organizzatori.");
    }

    @PostMapping("/mock/bookings/{bookingId}")
    public MockCheckoutService.Receipt pay(@PathVariable Long bookingId, @AuthenticationPrincipal AuthUser user) {
        if (!"TRAVELER".equals(user.role()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pagamento riservato ai viaggiatori.");
        return checkout.pay(bookingId, user.id());
    }

    @GetMapping("/organizer/statistics")
    public OrganizerStatisticsDTO statistics(@AuthenticationPrincipal AuthUser user) {
        requireOrganizer(user);
        return statistics.overview(user.id());
    }

    @GetMapping("/organizer/finance")
    public OrganizerFinanceDTO finance(@AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page) {
        requireOrganizer(user);
        return paymentService.finance(user.id(), page);
    }

    @GetMapping("/organizer/revenue")
    public ResponseEntity<BigDecimal> getOrganizerPaidRevenue(@AuthenticationPrincipal AuthUser user) {
        requireOrganizer(user);
        return ResponseEntity.ok(paymentService.getPaidAmountForOrganizer(user.id()));
    }
}
