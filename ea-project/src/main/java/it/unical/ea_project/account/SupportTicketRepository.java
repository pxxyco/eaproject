package it.unical.ea_project.account;

import java.util.*;
import org.springframework.data.domain.Pageable;

public interface SupportTicketRepository
        extends org.springframework.data.jpa.repository.JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserIdOrderByIdDesc(Long user, Pageable page);
}
