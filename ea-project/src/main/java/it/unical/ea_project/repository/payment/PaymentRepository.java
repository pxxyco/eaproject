package it.unical.ea_project.repository.payment;

import it.unical.ea_project.domain.payment.Payment;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  @Query("SELECT p FROM Payment p WHERE p.booking.user.id = :userId")
  List<Payment> findAllByUserId(Long id);

  @Query("select p from Payment p where p.booking.bookingId=:bookingId and p.deletedAt is null order by coalesce(p.paidAt,p.createdAt) desc,p.paymentId desc")
  List<Payment> findBookingPayments(@Param("bookingId") Long bookingId);

  @Query("select p from Payment p where p.booking.bookingId in :bookingIds and p.deletedAt is null order by coalesce(p.paidAt,p.createdAt) desc,p.paymentId desc")
  List<Payment> findBookingPaymentsBatch(@Param("bookingIds") List<Long> bookingIds);

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
          (t IS NOT NULL AND t.createdBy.id = :organizerId)
          OR
          (a IS NOT NULL AND a.createdBy.id = :organizerId)
        )
      """)
  BigDecimal sumPaidAmountForOrganizer(
      @Param("organizerId") Long organizerId,
      @Param("completed") Payment.PaymentStatus completed,
      @Param("confirmed") Payment.PaymentStatus confirmed);

  @Query("select p from Payment p join fetch p.booking b join fetch b.user left join fetch b.trip t left join fetch b.activity a where p.deletedAt is null and b.deletedAt is null and (t.createdBy.id=:organizer or a.createdBy.id=:organizer) order by coalesce(p.paidAt,p.createdAt) desc, p.paymentId desc")
  Page<Payment> organizerLedger(@Param("organizer") Long organizer, Pageable page);

  @Query("select coalesce(sum(p.amount),0) from Payment p join p.booking b left join b.trip t left join b.activity a where p.deletedAt is null and b.deletedAt is null and p.status=:status and (t.createdBy.id=:organizer or a.createdBy.id=:organizer)")
  BigDecimal organizerTotal(@Param("organizer") Long organizer, @Param("status") Payment.PaymentStatus status);

  @Query("select year(coalesce(p.paidAt,p.createdAt)),month(coalesce(p.paidAt,p.createdAt)),sum(case when b.trip is not null then p.amount else 0 end),sum(case when b.activity is not null then p.amount else 0 end),count(p) from Payment p join p.booking b left join b.trip t left join b.activity a where p.deletedAt is null and b.deletedAt is null and coalesce(p.paidAt,p.createdAt)>=:since and p.status in :paidStatuses and (t.createdBy.id=:organizer or a.createdBy.id=:organizer) group by year(coalesce(p.paidAt,p.createdAt)),month(coalesce(p.paidAt,p.createdAt)) order by year(coalesce(p.paidAt,p.createdAt)),month(coalesce(p.paidAt,p.createdAt))")
  List<Object[]> organizerMonthly(@Param("organizer") Long organizer, @Param("since") LocalDateTime since,
      @Param("paidStatuses") List<Payment.PaymentStatus> paidStatuses);

  @Transactional
  @Modifying
  @Query("UPDATE Payment p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.paymentId = :id")
  void softDeleteById(Long id);

}
