package it.unical.ea_project.service;

import java.math.BigDecimal;

public interface PaymentService {

    void elimina(Long id);

    BigDecimal getPaidAmountForOrganizer(Long organizerId);
}
