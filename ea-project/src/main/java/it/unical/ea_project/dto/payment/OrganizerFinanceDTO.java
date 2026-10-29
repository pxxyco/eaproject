package it.unical.ea_project.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrganizerFinanceDTO(BigDecimal paid, BigDecimal pending, BigDecimal cancelled,
        List<Entry> entries, int page, int totalPages, long totalElements) {
    public record Entry(String title, String participant, BigDecimal amount, String status, LocalDateTime date) {
    }
}
