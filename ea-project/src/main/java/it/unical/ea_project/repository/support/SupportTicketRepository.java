package it.unical.ea_project.repository.support;

import it.unical.ea_project.domain.support.SupportTicket;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository
        extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserIdOrderByIdDesc(Long user, Pageable page);
}
