package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.domain.User;
import it.unical.ea_project.repository.TripRepository;
import it.unical.ea_project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:organizer-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=DAY")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrganizerConcurrencyTest {
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired TripServiceImpl tripService;
    @Autowired BookingServiceImpl bookings;
    @Autowired PlatformTransactionManager transactionManager;

    @Test void simultaneousReservationsCannotOversell() throws Exception {
        var tx = new TransactionTemplate(transactionManager);
        long[] ids = tx.execute(status -> {
            User owner = new User(); owner.setUsername("parallel-owner"); owner.setEmail("parallel-owner@example.test"); owner.setRole(User.Role.ORGANIZER); users.save(owner);
            User traveler = new User(); traveler.setUsername("parallel-traveler"); traveler.setEmail("parallel-traveler@example.test"); traveler.setRole(User.Role.TRAVELER); users.save(traveler);
            Trip trip = new Trip(); trip.setTitle("Parallel Tour"); trip.setDestinationCity("Roma"); trip.setDestinationCountry("Italia");
            trip.setStartDate(LocalDate.of(2027,6,1)); trip.setEndDate(LocalDate.of(2027,6,2));
            trip.setTotalPrice(BigDecimal.TEN); trip.setMaxSeats(10); trip.setStatus(Trip.TripStatus.PUBLISHED);
            tripService.createTrip(trip, owner.getId());
            return new long[]{trip.getTripId(), traveler.getId()};
        });
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> reserve = () -> {
            ready.countDown(); start.await();
            try { bookings.creaPrenotazioneTrip(ids[1], ids[0], 7); return true; }
            catch (IllegalArgumentException expected) { return false; }
        };
        try {
            var first = executor.submit(reserve); var second = executor.submit(reserve);
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            assertEquals(1, List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)).stream().filter(Boolean::booleanValue).count());
            assertEquals(3, trips.findById(ids[0]).orElseThrow().getAvailableSeats());
        } finally { executor.shutdownNow(); }
    }
}
