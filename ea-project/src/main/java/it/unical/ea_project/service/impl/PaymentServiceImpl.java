package it.unical.ea_project.service.impl;


import it.unical.ea_project.repository.PaymentRepository;
import it.unical.ea_project.service.PaymentService;
import it.unical.ea_project.domain.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
                Payment.PaymentStatus.CONFIRMED
        );
    }
}