package it.unical.ea_project_javafx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {

    private Long paymentId;

    private Long bookingId;

    private BigDecimal amount;

    private String status;  // Payment.PaymentStatus: PENDING, COMPLETED, CONFIRMED, CANCELLED

    private String transactionId;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

}
