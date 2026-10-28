package it.unical.ea_project.service.payment.impl;

import it.unical.ea_project.domain.payment.Payment;
import it.unical.ea_project.repository.payment.PaymentRepository;
import it.unical.ea_project.service.payment.PaymentService;
import java.math.BigDecimal;
import it.unical.ea_project.dto.payment.OrganizerFinanceDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizerFinanceDTO finance(Long organizer, int page) {
        var rows = paymentRepository.organizerLedger(organizer, PageRequest.of(Math.max(0, Math.min(page, 10000)), 15));
        var entries = rows.getContent().stream().map(p -> {
            var b = p.getBooking();
            return new OrganizerFinanceDTO.Entry(
                    b.getTrip() != null ? b.getTrip().getTitle() : b.getActivity().getTitle(),
                    b.getUser().getFullName(), p.getAmount(), p.getStatus().name(),
                    p.getPaidAt() == null ? p.getCreatedAt() : p.getPaidAt());
        }).toList();
        return new OrganizerFinanceDTO(getPaidAmountForOrganizer(organizer),
                paymentRepository.organizerTotal(organizer, Payment.PaymentStatus.PENDING),
                paymentRepository.organizerTotal(organizer, Payment.PaymentStatus.CANCELLED),
                entries, rows.getNumber(), rows.getTotalPages(), rows.getTotalElements());
    }

    @Transactional
    public void elimina(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new RuntimeException("Payment non trovato");
        }
        paymentRepository.softDeleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getPaidAmountForOrganizer(Long organizerId) {
        return paymentRepository.sumPaidAmountForOrganizer(
                organizerId,
                Payment.PaymentStatus.COMPLETED,
                Payment.PaymentStatus.CONFIRMED);
    }
}
