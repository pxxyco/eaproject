package it.unical.ea_project.repository;


import it.unical.ea_project.domain.Payment;
import java.math.BigDecimal;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment,Long> {

    @Query("SELECT p FROM Payment p WHERE p.booking.user.id = :userId")
    List<Payment> findAllByUserId(Long id);

    Optional<Payment> findByBookingBookingId(Long bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        JOIN p.booking b
        LEFT JOIN b.trip t
        LEFT JOIN b.activity a
        WHERE p.deletedAt IS NULL
          AND b.deletedAt IS NULL
          AND p.status IN (:completed, :confirmed)
          AND (
            (t IS NOT NULL AND t.createdBy.id = :organizerId AND t.deletedAt IS NULL)
            OR
            (a IS NOT NULL AND a.createdBy.id = :organizerId AND a.deletedAt IS NULL)
          )
        """)
    BigDecimal sumPaidAmountForOrganizer(
            @Param("organizerId") Long organizerId,
            @Param("completed") Payment.PaymentStatus completed,
            @Param("confirmed") Payment.PaymentStatus confirmed
    );

    @Transactional
    @Modifying
    @Query("UPDATE Payment p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.paymentId = :id")
    void softDeleteById(Long id);

}
