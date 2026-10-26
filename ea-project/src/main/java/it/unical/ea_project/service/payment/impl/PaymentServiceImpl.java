package it.unical.ea_project.service.payment.impl;

import it.unical.ea_project.domain.payment.Payment;
import it.unical.ea_project.repository.payment.PaymentRepository;
import it.unical.ea_project.service.payment.PaymentService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
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
