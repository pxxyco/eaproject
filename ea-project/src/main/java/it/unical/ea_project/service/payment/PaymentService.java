package it.unical.ea_project.service.payment;

import java.math.BigDecimal;
import it.unical.ea_project.dto.payment.OrganizerFinanceDTO;

public interface PaymentService {

    OrganizerFinanceDTO finance(Long organizer, int page);

    void elimina(Long id);

    BigDecimal getPaidAmountForOrganizer(Long organizerId);
}
