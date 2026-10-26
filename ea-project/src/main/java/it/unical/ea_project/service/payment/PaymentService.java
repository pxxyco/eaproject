package it.unical.ea_project.service.payment;

import java.math.BigDecimal;

public interface PaymentService {

    void elimina(Long id);

    BigDecimal getPaidAmountForOrganizer(Long organizerId);
}
