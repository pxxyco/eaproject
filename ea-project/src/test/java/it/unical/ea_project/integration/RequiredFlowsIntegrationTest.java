package it.unical.ea_project.integration;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.booking.Booking;
import it.unical.ea_project.domain.payment.Payment;
import it.unical.ea_project.repository.payment.PaymentRepository;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import it.unical.ea_project.domain.activity.ActivityImage;
import it.unical.ea_project.domain.notification.Notification;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.trip.TripImage;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.repository.activity.ActivityImageRepository;
import it.unical.ea_project.repository.activity.ActivityRepository;
import it.unical.ea_project.repository.booking.BookingRepository;
import it.unical.ea_project.repository.notification.NotificationRepository;
import it.unical.ea_project.repository.trip.TripImageRepository;
import it.unical.ea_project.repository.trip.TripRepository;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.service.security.JwtService;
import it.unical.ea_project.service.booking.BookingHistoryService;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
                "spring.servlet.multipart.max-file-size=10MB", "spring.servlet.multipart.max-request-size=50MB" })
class RequiredFlowsIntegrationTest {
        @LocalServerPort
        int port;
        @Autowired
        PaymentRepository payments;
        @Autowired
        UserRepository users;
        @Autowired
        TripRepository trips;
        @Autowired
        ActivityRepository activities;
        @Autowired
        BookingRepository bookings;
        @Autowired
        TripImageRepository tripImages;
        @Autowired
        ActivityImageRepository activityImages;
        @Autowired
        NotificationRepository notifications;
        @Autowired
        JwtService jwt;
        @Autowired
        BookingHistoryService history;
        @Autowired
        JdbcTemplate jdbc;
        final HttpClient http = HttpClient.newHttpClient();
        User owner, otherOrganizer, traveler, secondTraveler;

        @BeforeEach
        void setup() {
                owner = user(User.Role.ORGANIZER);
                otherOrganizer = user(User.Role.ORGANIZER);
                traveler = user(User.Role.TRAVELER);
                secondTraveler = user(User.Role.TRAVELER);
        }

        User user(User.Role role) {
                User u = new User();
                String name = UUID.randomUUID().toString();
                u.setUsername(name);
                u.setEmail(name + "@example.test");
                u.setFullName(name);
                u.setRole(role);
                return users.save(u);
        }

        Trip trip(int seats) {
                Trip t = new Trip();
                t.setCreatedBy(owner);
                t.setTitle("Viaggio di prova");
                t.setDestinationCity("Roma");
                t.setStartDate(LocalDate.now().plusDays(3));
                t.setEndDate(LocalDate.now().plusDays(4));
                t.setTotalPrice(BigDecimal.TEN);
                t.setMaxSeats(seats);
                t.setAvailableSeats(seats);
                t.setStatus(Trip.TripStatus.PUBLISHED);
                return trips.save(t);
        }

        HttpResponse<String> call(String method, String path, String body, User user) throws Exception {
                var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                                .timeout(Duration.ofSeconds(15));
                if (user != null)
                        b.header("Authorization", "Bearer " + jwt.generateAccessToken(user));
                b.header("Content-Type", "application/json").method(method,
                                body == null ? HttpRequest.BodyPublishers.noBody()
                                                : HttpRequest.BodyPublishers.ofString(body));
                return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        }

        long id(HttpResponse<String> response, String field) {
                var m = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*(\\d+)").matcher(response.body());
                assertTrue(m.find(), response.body());
                return Long.parseLong(m.group(1));
        }

        @Test
        void protectedEndpointsRejectAnonymousAndForeignOwners() throws Exception {
                Trip t = trip(3);
                assertEquals(401, call("GET", "/api/bookings/mine", null, null).statusCode());
                String stage = "{\"title\":\"Tappa\",\"day\":1,\"orderInDay\":1,\"category\":\"VISIT\"}";
                assertEquals(403, call("POST", "/api/trips/" + t.getTripId() + "/stages", stage, otherOrganizer)
                                .statusCode());
                assertEquals(200, call("POST", "/api/trips/" + t.getTripId() + "/stages", stage, owner).statusCode());
                assertEquals(403, call("GET", "/api/trips/organizer/" + owner.getId(), null, otherOrganizer)
                                .statusCode());
                TripImage image = tripImages.save(new TripImage(null, t, "/photo/test.jpg", 1));
                assertEquals(403, call("DELETE", "/api/trip-images/" + image.getImageId(), null, otherOrganizer)
                                .statusCode());
                assertTrue(tripImages.existsById(image.getImageId()));
                assertEquals(204, call("DELETE", "/api/trip-images/" + image.getImageId(), null, owner).statusCode());
        }

        @Test
        void privateVisibilityRevokesSharesAndPendingInvitations() throws Exception {
                var created = call("POST", "/api/favorite-lists", "{\"name\":\"Lista\",\"visibility\":\"SHARED\"}",
                                traveler);
                assertEquals(201, created.statusCode(), created.body());
                long listId = id(created, "id");
                String invite = "{\"listId\":" + listId + ",\"identifier\":\"" + secondTraveler.getUsername()
                                + "\",\"permission\":\"VIEW\"}";
                assertEquals(201, call("POST", "/api/list-sharings", invite, traveler).statusCode());
                var notification = notifications
                                .findByUserAndTypeOrderByCreatedAtDesc(secondTraveler,
                                                Notification.NotificationType.LIST_INVITATION)
                                .getFirst();
                assertEquals(200,
                                call("POST", "/api/notifications/" + notification.getNotificationId() + "/accept", null,
                                                secondTraveler)
                                                .statusCode());
                assertEquals(200, call("GET", "/api/list-trips/list/" + listId, null, secondTraveler).statusCode());
                assertEquals(200, call("PUT", "/api/favorite-lists/" + listId,
                                "{\"name\":\"Lista\",\"visibility\":\"PRIVATE\"}", traveler).statusCode());
                assertEquals(403, call("GET", "/api/list-trips/list/" + listId, null, secondTraveler).statusCode());
                assertEquals(400, call("POST", "/api/list-sharings", invite, traveler).statusCode());
                assertEquals(200,
                                call("PUT", "/api/favorite-lists/" + listId,
                                                "{\"name\":\"Lista\",\"visibility\":\"SHARED\"}", traveler)
                                                .statusCode());
                assertEquals(403, call("GET", "/api/list-trips/list/" + listId, null, secondTraveler).statusCode());
                assertEquals(201, call("POST", "/api/list-sharings", invite, traveler).statusCode());
                var pending = notifications
                                .findByUserAndTypeOrderByCreatedAtDesc(secondTraveler,
                                                Notification.NotificationType.LIST_INVITATION)
                                .getFirst();
                assertEquals(200, call("PUT", "/api/favorite-lists/" + listId,
                                "{\"name\":\"Lista\",\"visibility\":\"PRIVATE\"}", traveler).statusCode());
                assertEquals(400,
                                call("POST", "/api/notifications/" + pending.getNotificationId() + "/accept", null,
                                                secondTraveler)
                                                .statusCode());
        }

        @Test
        void twoLegacyCopiesCannotBothBePaidEvenConcurrently() throws Exception {
                Trip t = trip(20); t.setAvailableSeats(18); trips.save(t);
                List<Long> ids = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                        Booking b = new Booking(); b.setUser(traveler); b.setTrip(t); b.setSeats(1); b.setPrice(BigDecimal.TEN);
                        b.setBookingType(Booking.BookingType.TRIP); b.setBookingStatus(Booking.BookingStatus.PENDING);
                        ids.add(bookings.save(b).getBookingId());
                }
                try (var pool = Executors.newFixedThreadPool(2)) {
                        CountDownLatch start = new CountDownLatch(1);
                        var first = pool.submit(() -> { start.await(); return call("POST", "/api/payments/mock/bookings/" + ids.get(0), null, traveler); });
                        var second = pool.submit(() -> { start.await(); return call("POST", "/api/payments/mock/bookings/" + ids.get(1), null, traveler); });
                        start.countDown();
                        var codes = List.of(first.get().statusCode(), second.get().statusCode());
                        assertEquals(1, codes.stream().filter(code -> code == 200).count());
                        assertEquals(1, codes.stream().filter(code -> code == 409).count());
                        assertEquals(1, payments.findBookingPaymentsBatch(ids).size());
                }
        }

        @Test
        void concurrentPaymentAndCancellationDoNotDeadlockOrRestoreSeatsTwice() throws Exception {
                Trip t = trip(10);
                long id = id(call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=1", null, traveler), "bookingId");
                try (var pool = Executors.newFixedThreadPool(2)) {
                        CountDownLatch start = new CountDownLatch(1);
                        var paid = pool.submit(() -> { start.await(); return call("POST", "/api/payments/mock/bookings/" + id, null, traveler); });
                        var cancelled = pool.submit(() -> { start.await(); return call("DELETE", "/api/bookings/" + id, null, traveler); });
                        start.countDown();
                        int paymentCode = paid.get().statusCode();
                        assertTrue(paymentCode == 200 || paymentCode == 409);
                        assertEquals(204, cancelled.get().statusCode());
                }
                assertEquals(10, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
                assertEquals(Booking.BookingStatus.CANCELLED, bookings.findById(id).orElseThrow().getBookingStatus());
                assertEquals(204, call("DELETE", "/api/bookings/" + id, null, traveler).statusCode());
                assertEquals(10, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
        }

        @Test
        void activityOccurrencesAreUniquePerUserAndFreeOccurrencesNeedNoPayment() throws Exception {
                Activity a = new Activity(); a.setCreatedBy(owner); a.setTitle("Visita ricorrente"); a.setCity("Roma");
                a.setCategory(Activity.Category.VISIT); a.setPrice(0d); a.setMaxSeats(10); a.setAvailableSeats(10);
                a.setStatus(Activity.Status.PUBLISHED); a.setStartDate(LocalDateTime.now().plusDays(2)); a.setEndDate(a.getStartDate().plusHours(1));
                a = activities.save(a);
                var booked = call("POST", "/api/bookings/activities/" + a.getActivityId() + "?seats=2", null, traveler);
                assertEquals(200, booked.statusCode(), booked.body()); assertTrue(booked.body().contains("CONFIRMED"));
                assertEquals(409, call("POST", "/api/bookings/activities/" + a.getActivityId() + "?seats=1", null, traveler).statusCode());
                Activity later = new Activity(); later.setCreatedBy(owner); later.setTitle(a.getTitle()); later.setCity("Roma");
                later.setCategory(Activity.Category.VISIT); later.setPrice(0d); later.setMaxSeats(10); later.setAvailableSeats(10);
                later.setStatus(Activity.Status.PUBLISHED); later.setStartDate(a.getStartDate().plusDays(1)); later.setEndDate(later.getStartDate().plusHours(1));
                later = activities.save(later);
                assertEquals(200, call("POST", "/api/bookings/activities/" + later.getActivityId() + "?seats=1", null, traveler).statusCode());
        }

        @Test
        void historyUsesDurationAndDistinguishesExpiredReservationsFromCompletedParticipation() {
                Activity a = new Activity(); a.setCreatedBy(owner); a.setTitle("Esperienza lunga"); a.setCity("Roma");
                a.setCategory(Activity.Category.VISIT); a.setPrice(0d); a.setMaxSeats(10); a.setAvailableSeats(9);
                a.setStatus(Activity.Status.PUBLISHED); a.setStartDate(LocalDateTime.now().minusMinutes(10)); a.setDurationMinutes(120);
                a = activities.save(a);
                Booking b = new Booking(); b.setUser(traveler); b.setActivity(a); b.setSeats(1); b.setPrice(BigDecimal.ZERO);
                b.setBookingType(Booking.BookingType.ACTIVITY); b.setBookingStatus(Booking.BookingStatus.CONFIRMED); b = bookings.save(b);
                assertEquals(0, history.page(traveler.getId(), "", "ALL", 0, 6).total());
                a.setStartDate(LocalDateTime.now().minusHours(3)); activities.save(a);
                var ended = history.page(traveler.getId(), "", "ALL", 0, 6);
                assertEquals(1, ended.total()); assertEquals(1, ended.completed()); assertEquals(1, ended.reviewable());
                b.setBookingStatus(Booking.BookingStatus.PENDING); bookings.save(b);
                var expired = history.page(traveler.getId(), "", "EXPIRED", 0, 6);
                assertEquals(1, expired.matched()); assertEquals(0, expired.completed()); assertEquals(0, expired.reviewable());
        }

        @Test
        void duplicateReservationsAreRejectedWithPlentyOfSeatsAndRebookingWorksAfterCancellation() throws Exception {
                Trip t = trip(20);
                String path = "/api/bookings/trips/" + t.getTripId() + "?seats=2";
                var first = call("POST", path, null, traveler);
                assertEquals(200, first.statusCode(), first.body());
                assertEquals(409, call("POST", path, null, traveler).statusCode());
                assertEquals(18, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
                assertTrue(call("GET", "/api/bookings/mine/experience?tripId=" + t.getTripId(), null, traveler).body().contains("bookingId"));
                assertEquals("[]", call("GET", "/api/bookings/mine/experience?tripId=" + t.getTripId(), null, secondTraveler).body());
                assertEquals(400, call("GET", "/api/bookings/mine/experience", null, traveler).statusCode());
                assertEquals(204, call("DELETE", "/api/bookings/" + id(first, "bookingId"), null, traveler).statusCode());
                assertEquals(200, call("POST", path, null, traveler).statusCode());
                assertEquals(18, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
        }

        @Test
        void concurrentDuplicateRequestsReserveOnlyOnceEvenWhenCapacityIsSufficient() throws Exception {
                Trip t = trip(20);
                String path = "/api/bookings/trips/" + t.getTripId() + "?seats=1";
                try (var pool = Executors.newFixedThreadPool(2)) {
                        CountDownLatch start = new CountDownLatch(1);
                        var first = pool.submit(() -> { start.await(); return call("POST", path, null, traveler); });
                        var second = pool.submit(() -> { start.await(); return call("POST", path, null, traveler); });
                        start.countDown();
                        var codes = List.of(first.get().statusCode(), second.get().statusCode());
                        assertEquals(1, codes.stream().filter(code -> code == 200).count());
                        assertEquals(1, codes.stream().filter(code -> code == 409).count());
                        assertEquals(19, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
                }
        }

        @Test
        void anOlderCompletedParticipationAllowsReviewEvenWithLaterPendingOrCancelledRecords() throws Exception {
                Trip t = trip(20);
                var booked = call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=1", null, traveler);
                assertEquals(200, call("POST", "/api/payments/mock/bookings/" + id(booked, "bookingId"), null, traveler).statusCode());
                t.setStartDate(LocalDate.now().minusDays(4)); t.setEndDate(LocalDate.now().minusDays(3)); trips.save(t);
                for (int i = 0; i < 4; i++) {
                        Booking old = new Booking(); old.setUser(traveler); old.setTrip(t); old.setSeats(1); old.setPrice(BigDecimal.TEN);
                        old.setBookingType(Booking.BookingType.TRIP);
                        old.setBookingStatus(i % 2 == 0 ? Booking.BookingStatus.PENDING : Booking.BookingStatus.CANCELLED);
                        bookings.save(old);
                }
                var review = call("POST", "/api/reviews", "{\"tripId\":" + t.getTripId() + ",\"rating\":4}", traveler);
                assertEquals(200, review.statusCode(), review.body());
                assertEquals(200, call("PUT", "/api/reviews/" + id(review, "reviewId"), "{\"rating\":5,\"comment\":\"Esperienza aggiornata\"}", traveler).statusCode());
        }

        @Test
        void bookingAwaitsPaymentAndCancellationRestoresSeatsOnce() throws Exception {
                Trip t = trip(2);
                String path = "/api/bookings/trips/" + t.getTripId() + "?seats=1";
                var booked = call("POST", path, null, traveler);
                assertEquals(200, booked.statusCode(), booked.body());
                assertTrue(booked.body().contains("PENDING"));
                long bookingId = id(booked, "bookingId");
                assertEquals(403, call("DELETE", "/api/bookings/" + bookingId, null, secondTraveler).statusCode());
                assertEquals(204, call("DELETE", "/api/bookings/" + bookingId, null, traveler).statusCode());
                assertEquals(204, call("DELETE", "/api/bookings/" + bookingId, null, traveler).statusCode());
                assertEquals(2, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
        }

        @Test
        void lastSeatCannotBeSoldTwice() throws Exception {
                Trip t = trip(1);
                String path = "/api/bookings/trips/" + t.getTripId() + "?seats=1";
                try (var executor = Executors.newFixedThreadPool(2)) {
                        CountDownLatch start = new CountDownLatch(1);
                        var first = executor.submit(() -> {
                                start.await();
                                return call("POST", path, null, traveler).statusCode();
                        });
                        var second = executor.submit(() -> {
                                start.await();
                                return call("POST", path, null, secondTraveler).statusCode();
                        });
                        start.countDown();
                        assertEquals(List.of(200, 400), Stream.of(first.get(), second.get()).sorted().toList());
                }
                assertEquals(0, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
        }

        @Test
        void cancelledOrPastTripsCannotBeBooked() throws Exception {
                Trip t = trip(2);
                t.setStatus(Trip.TripStatus.CANCELLED);
                trips.save(t);
                assertEquals(400,
                                call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=1", null, traveler)
                                                .statusCode());
                t.setStatus(Trip.TripStatus.PUBLISHED);
                t.setStartDate(LocalDate.now().minusDays(1));
                trips.save(t);
                assertEquals(400,
                                call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=1", null, traveler)
                                                .statusCode());
        }

        @Test
        void activityReservationAndImageOwnershipUseSameRules() throws Exception {
                Activity a = new Activity();
                a.setCreatedBy(owner);
                a.setTitle("Visita");
                a.setCategory(Activity.Category.VISIT);
                a.setCity("Roma");
                a.setStartDate(LocalDateTime.now().plusDays(2));
                a.setEndDate(LocalDateTime.now().plusDays(2).plusHours(1));
                a.setPrice(12d);
                a.setMaxSeats(1);
                a.setAvailableSeats(1);
                a.setStatus(Activity.Status.PUBLISHED);
                a = activities.save(a);
                ActivityImage image = activityImages.save(new ActivityImage(null, a, "/photo/test.jpg", 1));
                assertEquals(403,
                                call("DELETE", "/api/activity-images/" + image.getImageId(), null, otherOrganizer)
                                                .statusCode());
                assertEquals(204,
                                call("DELETE", "/api/activity-images/" + image.getImageId(), null, owner).statusCode());
                var booked = call("POST", "/api/bookings/activities/" + a.getActivityId() + "?seats=1", null, traveler);
                assertEquals(200, booked.statusCode(), booked.body());
                assertTrue(booked.body().contains("PENDING"));
                assertEquals(400,
                                call("POST", "/api/bookings/activities/" + a.getActivityId() + "?seats=1", null,
                                                secondTraveler)
                                                .statusCode());
        }

        @Test
        void organizerCrudPreservesReservedSeatsAndItinerary() throws Exception {
                Trip t = trip(3);
                assertEquals(200,
                                call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=2", null, traveler)
                                                .statusCode());
                String update = "{\"title\":\"Viaggio aggiornato\",\"destinationCity\":\"Roma\",\"startDate\":\""
                                + t.getStartDate()
                                + "\",\"endDate\":\"" + t.getEndDate()
                                + "\",\"totalPrice\":10,\"maxSeats\":4,\"availableSeats\":999,\"status\":\"PUBLISHED\"}";
                assertEquals(403, call("PUT", "/api/trips/" + t.getTripId(), update, otherOrganizer).statusCode());
                var response = call("PUT", "/api/trips/" + t.getTripId(), update, owner);
                assertEquals(200, response.statusCode(), response.body());
                assertEquals(2, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
                assertEquals(400,
                                call("PUT", "/api/trips/" + t.getTripId(),
                                                update.replace("\"maxSeats\":4", "\"maxSeats\":1"), owner)
                                                .statusCode());
                assertEquals(200, call("POST", "/api/trips/" + t.getTripId() + "/stages",
                                "{\"title\":\"Museo\",\"day\":1,\"orderInDay\":1,\"category\":\"VISIT\"}", owner)
                                .statusCode());
                var detail = call("GET", "/api/trips/" + t.getTripId(), null, null);
                assertEquals(200, detail.statusCode());
                assertTrue(detail.body().contains("Museo"), detail.body());
                assertEquals(200, call("GET", "/api/trips/organizer/" + owner.getId(), null, owner).statusCode());
                assertEquals(403, call("DELETE", "/api/trips/" + t.getTripId(), null, otherOrganizer).statusCode());
                assertEquals(204, call("DELETE", "/api/trips/" + t.getTripId(), null, owner).statusCode());
                assertEquals(404, call("GET", "/api/trips/" + t.getTripId(), null, null).statusCode());
                assertEquals(Trip.TripStatus.CANCELLED, trips.findById(t.getTripId()).orElseThrow().getStatus());
                assertTrue(call("GET", "/api/bookings/mine", null, traveler).body()
                                .contains("\"itemStatus\":\"CANCELLED\""));
        }

        @Test
        void registrationLoginRefreshAndIdentityAreConnected() throws Exception {
                String name = "mobile_" + UUID.randomUUID().toString().substring(0, 8);
                String registration = "{\"username\":\"" + name + "\",\"email\":\"" + name
                                + "@example.test\",\"fullName\":\"Mobile Test\",\"password\":\"TestPassword!42\",\"role\":\"TRAVELER\"}";
                var registered = call("POST", "/api/users", registration, null);
                assertEquals(201, registered.statusCode(), registered.body());
                var login = call("POST", "/api/users/login",
                                "{\"identifier\":\"" + name + "\",\"password\":\"TestPassword!42\"}", null);
                assertEquals(200, login.statusCode());
                String access = login.headers().firstValue("Authorization").orElseThrow();
                String refresh = login.headers().firstValue("X-Refresh-Token").orElseThrow();
                var me = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/users/me"))
                                .header("Authorization", access).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, me.statusCode());
                assertTrue(me.body().contains(name));
                var renewed = http.send(
                                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/users/refresh"))
                                                .header("X-Refresh-Token", refresh)
                                                .POST(HttpRequest.BodyPublishers.noBody()).build(),
                                HttpResponse.BodyHandlers.ofString());
                assertEquals(200, renewed.statusCode());
                assertTrue(renewed.headers().firstValue("Authorization").orElseThrow().startsWith("Bearer "));
                String profile = "{\"username\":\"" + name + "\",\"email\":\"updated_" + name
                                + "@example.test\",\"fullName\":\"Updated User\"}";
                var updated = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/users/me"))
                                .header("Authorization", access).header("Content-Type", "application/json")
                                .PUT(HttpRequest.BodyPublishers.ofString(profile)).build(),
                                HttpResponse.BodyHandlers.ofString());
                assertEquals(200, updated.statusCode(), updated.body());
                assertTrue(updated.body().contains("Updated User"));
                var legacy = call("POST", "/api/users",
                                registration.replace(name, name + "b").replace("\"role\":",
                                                "\"oauthProvider\":false,\"role\":"),
                                null);
                assertEquals(201, legacy.statusCode(), legacy.body());
        }

        @Test
        void mobileCanCreateOffersWithoutOptionalCollections() throws Exception {
                String start = LocalDate.now().plusDays(4).toString();
                String end = LocalDate.now().plusDays(5).toString();
                String tripBody = "{\"title\":\"Offerta mobile\",\"destinationCity\":\"Roma\",\"destinationCountry\":\"Italia\",\"startDate\":\""
                                + start + "\",\"endDate\":\"" + end
                                + "\",\"totalPrice\":10,\"maxSeats\":3,\"status\":\"PUBLISHED\"}";
                var createdTrip = call("POST", "/api/trips", tripBody, owner);
                assertEquals(200, createdTrip.statusCode(), createdTrip.body());
                assertTrue(createdTrip.body().contains("\"stages\":[]"), createdTrip.body());
                String activityBody = "{\"title\":\"Attività mobile\",\"city\":\"Roma\",\"category\":\"VISIT\",\"startDate\":\""
                                + start + "T10:00:00\",\"endDate\":\"" + start
                                + "T11:00:00\",\"price\":10,\"maxSeats\":3}";
                var createdActivity = call("POST", "/api/activities", activityBody, owner);
                assertEquals(201, createdActivity.statusCode(), createdActivity.body());
                assertEquals(200, call("GET", "/api/activities/organizer/mine", null, owner).statusCode());
                assertEquals(403,
                                call("DELETE", "/api/activities/" + id(createdActivity, "activityId"), null,
                                                otherOrganizer)
                                                .statusCode());
                assertEquals(204,
                                call("DELETE", "/api/activities/" + id(createdActivity, "activityId"), null, owner)
                                                .statusCode());
        }

        @Test
        void quickFavoritesArePrivateAndConcurrentAddsAreIdempotent() throws Exception {
                Trip trip = trip(3);
                String target = "/api/favorites/trips/" + trip.getTripId();
                assertEquals(401, call("GET", "/api/favorites", null, null).statusCode());
                try (var pool = Executors.newFixedThreadPool(2)) {
                        var first = pool.submit(() -> call("POST", target, null, traveler));
                        var second = pool.submit(() -> call("POST", target, null, traveler));
                        assertEquals(200, first.get().statusCode());
                        assertEquals(200, second.get().statusCode());
                }
                String saved = call("GET", "/api/favorites", null, traveler).body();
                assertEquals(1, Pattern.compile("\"itemId\"").matcher(saved).results().count());
                assertTrue(saved.contains("Viaggio di prova"));
                assertEquals(200, call("POST", target, null, traveler).statusCode());
                assertEquals(saved, call("GET", "/api/favorites", null, traveler).body());
                assertEquals("[]", call("GET", "/api/favorites", null, secondTraveler).body());
                assertEquals(200, call("DELETE", target, null, secondTraveler).statusCode());
                assertTrue(call("GET", target, null, traveler).body().contains("true"));
                assertEquals(200, call("DELETE", target, null, traveler).statusCode());
                assertEquals(200, call("DELETE", target, null, traveler).statusCode());
                assertEquals("[]", call("GET", "/api/favorites", null, traveler).body());
        }

        @Test
        void quickFavoritesRejectUnavailableOffersAndMapActivities() throws Exception {
                Trip unpublished = trip(2);
                unpublished.setStatus(Trip.TripStatus.DRAFT);
                trips.save(unpublished);
                assertEquals(400, call("POST", "/api/favorites/trips/" + unpublished.getTripId(), null, traveler)
                                .statusCode());
                assertEquals(400, call("POST", "/api/favorites/trips/999999999", null, traveler).statusCode());
                Activity activity = new Activity();
                activity.setCreatedBy(owner);
                activity.setTitle("Visita preferita");
                activity.setCity("Roma");
                activity.setCategory(Activity.Category.VISIT);
                activity.setPrice(10.0);
                activity.setStatus(Activity.Status.PUBLISHED);
                activity = activities.save(activity);
                String path = "/api/favorites/activities/" + activity.getActivityId();
                assertEquals(200, call("POST", path, null, traveler).statusCode());
                String saved = call("GET", "/api/favorites", null, traveler).body();
                assertTrue(saved.contains("Visita preferita"));
                assertTrue(saved.contains("ACTIVITY"));
                activity.setDeletedAt(LocalDateTime.now());
                activities.save(activity);
                assertTrue(call("GET", "/api/favorites", null, traveler).body()
                                .contains("Attività non più disponibile"));
                assertEquals(200, call("DELETE", path, null, traveler).statusCode());
        }

        @Test
        void removingQuickFavoriteKeepsNamedCollectionsIntact() throws Exception {
                Trip trip = trip(3);
                String path = "/api/favorites/trips/" + trip.getTripId();
                assertEquals(200, call("POST", path, null, traveler).statusCode());
                var collection = call("POST", "/api/favorite-lists",
                                "{\"name\":\"Raccolta indipendente\",\"visibility\":\"PRIVATE\"}", traveler);
                assertEquals(201, collection.statusCode());
                long list = id(collection, "id");
                assertEquals("[]", call("GET", "/api/list-trips/list/" + list, null, traveler).body());
                assertEquals(201, call("POST", "/api/list-trips",
                                "{\"listId\":" + list + ",\"tripId\":" + trip.getTripId() + "}", traveler)
                                .statusCode());
                assertEquals(200, call("DELETE", path, null, traveler).statusCode());
                assertTrue(call("GET", "/api/list-trips/list/" + list, null, traveler).body()
                                .contains("Viaggio di prova"));
        }

        long chat(User traveler, User organizer) throws Exception {
                var response = call("POST", "/api/chat/conversations", "{\"organizerId\":" + organizer.getId() + "}",
                                traveler);
                assertEquals(200, response.statusCode(), response.body());
                return id(response, "id");
        }

        HttpResponse<String> message(long chat, User sender, String body, Long reply) throws Exception {
                return call("POST", "/api/chat/conversations/" + chat + "/messages",
                                "{\"clientId\":\"" + UUID.randomUUID() + "\",\"body\":\"" + body + "\"" +
                                                (reply == null ? "" : ",\"replyToId\":" + reply) + "}",
                                sender);
        }

        HttpResponse<String> photo(long chat, User sender, byte[] image, String clientId, Long reply) throws Exception {
                String boundary = "EAProjectTestBoundary";
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bytes.write(
                                ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"clientId\"\r\n\r\n"
                                                + clientId + "\r\n")
                                                .getBytes(StandardCharsets.UTF_8));
                if (reply != null)
                        bytes.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"replyToId\"\r\n\r\n"
                                        + reply
                                        + "\r\n").getBytes(StandardCharsets.UTF_8));
                bytes.write(("--" + boundary
                                + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"screen.png\"\r\nContent-Type: image/png\r\n\r\n")
                                .getBytes(StandardCharsets.UTF_8));
                bytes.write(image);
                bytes.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                return http.send(
                                HttpRequest
                                                .newBuilder(
                                                                URI.create("http://127.0.0.1:" + port
                                                                                + "/api/chat/conversations/" + chat
                                                                                + "/photos"))
                                                .header("Authorization", "Bearer " + jwt.generateAccessToken(sender))
                                                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                                                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray()))
                                                .build(),
                                HttpResponse.BodyHandlers.ofString());
        }

        @Test
        void chatDeletionIsPersonalAndNewMessagesRestoreOnlyNewHistory() throws Exception {
                long c = chat(traveler, owner);
                var first = message(c, owner, "Primo messaggio", null);
                assertEquals(200, first.statusCode(), first.body());
                String base = "/api/chat/conversations/" + c;
                assertEquals(403, call("GET", base + "/messages", null, secondTraveler).statusCode());
                assertEquals(403, call("DELETE", base, null, secondTraveler).statusCode());
                assertEquals(204, call("DELETE", base, null, traveler).statusCode());
                assertEquals("[]", call("GET", "/api/chat/conversations", null, traveler).body());
                assertEquals(200,
                                call("POST", "/api/chat/conversations", "{\"travelerId\":" + traveler.getId() + "}",
                                                owner)
                                                .statusCode());
                assertEquals("[]", call("GET", "/api/chat/conversations", null, traveler).body());
                assertEquals("[]", call("GET", base + "/messages", null, traveler).body());
                assertTrue(call("GET", base + "/messages", null, owner).body().contains("Primo messaggio"));
                assertEquals(200, message(c, owner, "Dopo eliminazione", null).statusCode());
                String history = call("GET", base + "/messages", null, traveler).body();
                assertTrue(history.contains("Dopo eliminazione"));
                assertTrue(!history.contains("Primo messaggio"));
                assertTrue(call("GET", "/api/chat/conversations", null, traveler).body().contains("Dopo eliminazione"));
        }

        @Test
        void replyMustBelongToVisibleHistoryAndRetriesCannotChangeIt() throws Exception {
                long first = chat(traveler, owner), second = chat(traveler, otherOrganizer);
                long original = id(message(first, owner, "Domanda", null), "id");
                assertEquals(404, message(second, traveler, "Risposta", original).statusCode());
                var reply = message(first, traveler, "Risposta", original);
                assertEquals(200, reply.statusCode(), reply.body());
                assertTrue(reply.body().contains("\"replyTo\""));
                assertTrue(reply.body().contains("Domanda"));
                String client = UUID.randomUUID().toString();
                String path = "/api/chat/conversations/" + first + "/messages";
                String payload = "{\"clientId\":\"" + client + "\",\"body\":\"Confermo\",\"replyToId\":" + original
                                + "}";
                long id = id(call("POST", path, payload, traveler), "id");
                assertEquals(id, id(call("POST", path, payload, traveler), "id"));
                assertEquals(409, call("POST", path, payload.replace(",\"replyToId\":" + original, ""), traveler)
                                .statusCode());
                call("DELETE", "/api/chat/conversations/" + first, null, traveler);
                assertEquals(404, message(first, traveler, "Non visibile", original).statusCode());
                assertEquals(410, call("POST", path, payload, traveler).statusCode());
                assertEquals(200, message(first, owner, "Contesto nuovo", original).statusCode());
                String afterDelete = call("GET", path, null, traveler).body();
                assertTrue(afterDelete.contains("Contesto nuovo"));
                assertTrue(!afterDelete.contains("Domanda"));
        }

        @Test
        void partialReadDoesNotDismissNewerChatNotifications() throws Exception {
                long c = chat(traveler, owner);
                long first = id(message(c, owner, "Uno", null), "id");
                long last = id(message(c, owner, "Due", null), "id");
                String path = "/api/chat/conversations/" + c + "/read";
                assertEquals(204, call("POST", path, "{\"throughId\":" + first + "}", traveler).statusCode());
                assertTrue(notifications
                                .findFirstByUser_IdAndRelatedEntityTypeAndRelatedEntityIdAndReadFalse(traveler.getId(),
                                                "CHAT", c)
                                .isPresent());
                assertEquals(204, call("POST", path, "{\"throughId\":" + last + "}", traveler).statusCode());
                assertTrue(notifications
                                .findFirstByUser_IdAndRelatedEntityTypeAndRelatedEntityIdAndReadFalse(traveler.getId(),
                                                "CHAT", c)
                                .isEmpty());
        }

        @Test
        void photosAreValidatedPrivateAndIdempotent() throws Exception {
                long c = chat(traveler, owner);
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                ImageIO.write(new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB), "png", png);
                String client = UUID.randomUUID().toString();
                var sent = photo(c, traveler, png.toByteArray(), client, null);
                assertEquals(200, sent.statusCode(), sent.body());
                long m = id(sent, "id");
                assertTrue(sent.body().contains("\"hasPhoto\":true"));
                assertEquals(m, id(photo(c, traveler, png.toByteArray(), client, null), "id"));
                String path = "/api/chat/conversations/" + c + "/messages/" + m + "/photo";
                var loaded = call("GET", path, null, owner);
                assertEquals(200, loaded.statusCode());
                assertTrue(loaded.body().contains("data"));
                assertEquals(403, call("GET", path, null, secondTraveler).statusCode());
                assertEquals(401, call("GET", path, null, null).statusCode());
                assertEquals(400,
                                photo(c, traveler, "<svg/>".getBytes(StandardCharsets.UTF_8),
                                                UUID.randomUUID().toString(), null)
                                                .statusCode());
                assertEquals(409, photo(c, traveler, new byte[] { 1, 2 }, client, null).statusCode());
                ByteArrayOutputStream wide = new ByteArrayOutputStream();
                ImageIO.write(new BufferedImage(10001, 1, BufferedImage.TYPE_INT_RGB), "png", wide);
                assertEquals(400, photo(c, traveler, wide.toByteArray(), UUID.randomUUID().toString(), null)
                                .statusCode());
                assertEquals(413,
                                photo(c, traveler, new byte[8 * 1024 * 1024 + 1], UUID.randomUUID().toString(), null)
                                                .statusCode());
                call("DELETE", "/api/chat/conversations/" + c, null, owner);
                assertEquals(404, call("GET", path, null, owner).statusCode());
        }

        @Test
        void organizerCanStartOnlyWithItsOwnBookedTravelers() throws Exception {
                String body = "{\"travelerId\":" + traveler.getId() + "}";
                assertEquals(403, call("POST", "/api/chat/conversations", body, owner).statusCode());
                Trip t = trip(5);
                Booking b = new Booking();
                b.setUser(traveler);
                b.setTrip(t);
                b.setSeats(1);
                b.setPrice(BigDecimal.TEN);
                b.setBookingType(Booking.BookingType.TRIP);
                b.setBookingStatus(Booking.BookingStatus.CONFIRMED);
                bookings.save(b);
                assertEquals(200, call("POST", "/api/chat/conversations", body, owner).statusCode());
                assertEquals(403, call("POST", "/api/chat/conversations", body, otherOrganizer).statusCode());
        }

        @Test
        void financialLedgerIsOrganizerScopedAndCountsActualPaymentsOnly() throws Exception {
                Trip t = trip(5);
                Booking b = new Booking();
                b.setUser(traveler);
                b.setTrip(t);
                b.setSeats(1);
                b.setPrice(BigDecimal.valueOf(999));
                b.setBookingType(Booking.BookingType.TRIP);
                b.setBookingStatus(Booking.BookingStatus.CONFIRMED);
                bookings.save(b);
                for (int i = 0; i < 17; i++) {
                        Payment payment = new Payment();
                        payment.setBooking(b);
                        payment.setAmount(BigDecimal.TEN);
                        payment.setStatus(i == 16 ? Payment.PaymentStatus.PENDING : Payment.PaymentStatus.COMPLETED);
                        payments.save(payment);
                }
                var ledger = call("GET", "/api/payments/organizer/finance", null, owner);
                assertEquals(200, ledger.statusCode(), ledger.body());
                assertTrue(ledger.body().contains("\"paid\":160"));
                assertTrue(ledger.body().contains("\"pending\":10"));
                assertTrue(ledger.body().contains("\"totalPages\":2"));
                assertTrue(ledger.body().contains("\"totalElements\":17"));
                assertTrue(!call("GET", "/api/payments/organizer/finance", null, otherOrganizer).body()
                                .contains("Viaggio di prova"));
                assertEquals(403, call("GET", "/api/payments/organizer/finance", null, traveler).statusCode());
                assertEquals(401, call("GET", "/api/payments/organizer/finance", null, null).statusCode());
        }

        @Test
        void mockCheckoutUsesStoredAmountAndCannotChargeTwice() throws Exception {
                Trip trip = trip(8);
                var booked = call("POST", "/api/bookings/trips/" + trip.getTripId() + "?seats=2", null, traveler);
                assertEquals(200, booked.statusCode(), booked.body());
                long booking = id(booked, "bookingId");
                String path = "/api/payments/mock/bookings/" + booking;
                assertEquals(403, call("POST", path, "{}", secondTraveler).statusCode());
                assertEquals(403, call("POST", path, "{}", owner).statusCode());
                Payment pending = new Payment();
                pending.setBooking(bookings.findById(booking).orElseThrow());
                pending.setAmount(BigDecimal.valueOf(999));
                pending.setStatus(Payment.PaymentStatus.PENDING);
                payments.save(pending);
                jdbc.update("update \"PAYMENT\" set created_at=? where payment_id=?",
                                LocalDateTime.now().minusMonths(13),
                                pending.getPaymentId());
                var paid = call("POST", path, "{\"amount\":0}", traveler);
                assertEquals(200, paid.statusCode(), paid.body());
                assertTrue(paid.body().contains("\"amount\":20"));
                assertTrue(paid.body().contains("MOCK-"));
                var stats = call("GET", "/api/payments/organizer/statistics", null, owner);
                assertTrue(stats.body().contains("\"periodPaid\":20"), stats.body());
                assertEquals(200, call("POST", path, "{}", traveler).statusCode());
                var records = payments.findBookingPayments(booking);
                assertEquals(1, records.size());
                assertEquals(0, BigDecimal.valueOf(20).compareTo(records.getFirst().getAmount()));
                assertEquals(Payment.PaymentStatus.CONFIRMED, records.getFirst().getStatus());
                assertTrue(
                                call("GET", "/api/bookings/mine", null, traveler).body()
                                                .contains("\"paymentStatus\":\"CONFIRMED\""));
        }

        @Test
        void concurrentMockCheckoutIsIdempotentAndCancelledBookingCannotBePaid() throws Exception {
                Trip trip = trip(8);
                long booking = id(call("POST", "/api/bookings/trips/" + trip.getTripId() + "?seats=1", null, traveler),
                                "bookingId");
                String path = "/api/payments/mock/bookings/" + booking;
                var start = new CountDownLatch(1);
                try (var executor = Executors.newFixedThreadPool(4)) {
                        var tasks = IntStream.range(0, 4).mapToObj(i -> executor.submit(() -> {
                                start.await();
                                return call("POST", path, "{}", traveler).statusCode();
                        })).toList();
                        start.countDown();
                        for (var future : tasks)
                                assertEquals(200, future.get());
                }
                assertEquals(1, payments.findBookingPayments(booking).size());
                long cancelled = id(
                                call("POST", "/api/bookings/trips/" + trip.getTripId() + "?seats=1", null,
                                                secondTraveler),
                                "bookingId");
                assertEquals(204, call("DELETE", "/api/bookings/" + cancelled, null, secondTraveler).statusCode());
                assertEquals(409, call("POST", "/api/payments/mock/bookings/" + cancelled, "{}", secondTraveler)
                                .statusCode());
        }

        @Test
        void organizerStatisticsSeparateTripAndActivityReceiptsAndPreserveOwnership() throws Exception {
                Trip trip = trip(8);
                long booking = id(call("POST", "/api/bookings/trips/" + trip.getTripId() + "?seats=2", null, traveler),
                                "bookingId");
                assertEquals(200, call("POST", "/api/payments/mock/bookings/" + booking, "{}", traveler).statusCode());
                Activity activity = new Activity();
                activity.setCreatedBy(owner);
                activity.setTitle("Visita di prova");
                activity.setPrice(7.5);
                activity.setCity("Roma");
                activity.setCategory(Activity.Category.VISIT);
                activity.setStartDate(LocalDateTime.now().plusDays(3));
                activity.setEndDate(LocalDateTime.now().plusDays(3).plusHours(1));
                activity.setMaxSeats(5);
                activity.setAvailableSeats(5);
                activity.setStatus(Activity.Status.PUBLISHED);
                activities.save(activity);
                long activityBooking = id(
                                call("POST", "/api/bookings/activities/" + activity.getActivityId() + "?seats=1", null,
                                                traveler),
                                "bookingId");
                assertEquals(200, call("POST", "/api/payments/mock/bookings/" + activityBooking, "{}", traveler)
                                .statusCode());
                var statistics = call("GET", "/api/payments/organizer/statistics", null, owner);
                assertEquals(200, statistics.statusCode(), statistics.body());
                assertTrue(statistics.body().contains("\"periodPaid\":27.50"), statistics.body());
                assertTrue(statistics.body().contains("\"paymentCount\":2"));
                assertEquals(12, statistics.body().split("\"month\"").length - 1);
                assertTrue(statistics.body().contains("\"trips\":20.00"));
                assertTrue(statistics.body().contains("\"activities\":7.50"));
                assertTrue(call("GET", "/api/payments/organizer/statistics", null, otherOrganizer).body()
                                .contains("\"paymentCount\":0"));
                assertEquals(403, call("GET", "/api/payments/organizer/statistics", null, traveler).statusCode());
        }

        @Test
        void readinessAndLegacyCalendarHaveWorkingContracts() throws Exception {
                assertEquals(200, call("GET", "/api/health", null, null).statusCode());
                Trip t = trip(3);
                t.setTitle("Città, musei; " + "è".repeat(100));
                t = trips.save(t);
                var calendar = call("GET", "/api/v1/trips/" + t.getTripId() + "/export/ics", null, traveler);
                assertEquals(200, calendar.statusCode(), calendar.body());
                assertTrue(calendar.body().contains("BEGIN:VCALENDAR"));
                assertTrue(calendar.body().contains("Città\\, musei\\;"));
                for (String line : calendar.body().split("\r\n"))
                        assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 75, line);
                t.setStatus(Trip.TripStatus.DRAFT);
                trips.save(t);
                String path = "/api/v1/trips/" + t.getTripId() + "/export/ics";
                assertEquals(403, call("GET", path, null, traveler).statusCode());
                assertEquals(200, call("GET", path, null, owner).statusCode());
                assertEquals(401, call("GET", path, null, null).statusCode());
        }

        @Test
        void collaborativePlanEnforcesPermissionsAndStaleVersions() throws Exception {
                long list = id(call("POST", "/api/favorite-lists", "{\"name\":\"Piano RC\",\"visibility\":\"SHARED\"}",
                                traveler), "id");
                String path = "/api/itineraries/" + list;
                String stop = "{\"title\":\"Museo\",\"day\":1,\"position\":1,\"version\":0}";
                var added = call("POST", path + "/stops", stop, traveler);
                assertEquals(201, added.statusCode(), added.body());
                long stopId = id(added, "id");
                assertEquals(403, call("GET", path, null, secondTraveler).statusCode());
                String invite = "{\"listId\":" + list + ",\"identifier\":\"" + secondTraveler.getUsername()
                                + "\",\"permission\":\"EDIT\"}";
                assertEquals(201, call("POST", "/api/list-sharings", invite, traveler).statusCode());
                var invitation = notifications.findByUserAndTypeOrderByCreatedAtDesc(secondTraveler,
                                Notification.NotificationType.LIST_INVITATION).getFirst();
                assertEquals(200, call("POST", "/api/notifications/" + invitation.getNotificationId() + "/accept", null,
                                secondTraveler).statusCode());
                assertEquals(200, call("PUT", path + "/stops/" + stopId, stop.replace("Museo", "Parco"), secondTraveler)
                                .statusCode());
                assertEquals(409, call("PUT", path + "/stops/" + stopId, stop, traveler).statusCode());
                assertEquals(403, call("POST", path + "/stops", stop, owner).statusCode());
        }

        @Test
        void recommendationsExcludeAlreadyBookedExperiencesAndAreTravelerOnly() throws Exception {
                Trip booked = trip(3), available = trip(3);
                call("POST", "/api/bookings/trips/" + booked.getTripId() + "?seats=1", null, traveler);
                var response = call("GET", "/api/recommendations/mine?limit=24", null, traveler);
                assertEquals(200, response.statusCode(), response.body());
                assertFalse(response.body().contains("\"tripId\":" + booked.getTripId() + ","));
                assertTrue(response.body().contains("\"tripId\":" + available.getTripId() + ","));
                assertEquals(403, call("GET", "/api/recommendations/mine", null, owner).statusCode());
        }

        @Test
        void logoutRevokesBothTokenTypesAndRefreshCannotAuthenticateApi() throws Exception {
                String access = jwt.generateAccessToken(traveler), refresh = jwt.generateRefreshToken(traveler);
                String url = "http://127.0.0.1:" + port;
                var asAccess = http.send(HttpRequest.newBuilder(URI.create(url + "/api/users/me"))
                                .header("Authorization", "Bearer " + refresh).GET().build(),
                                HttpResponse.BodyHandlers.ofString());
                assertEquals(401, asAccess.statusCode());
                assertEquals(204, call("POST", "/api/users/logout", null, traveler).statusCode());
                var revoked = http.send(HttpRequest.newBuilder(URI.create(url + "/api/users/me"))
                                .header("Authorization", "Bearer " + access).GET().build(),
                                HttpResponse.BodyHandlers.ofString());
                assertEquals(401, revoked.statusCode());
                var refreshAttempt = http.send(HttpRequest.newBuilder(URI.create(url + "/api/users/refresh"))
                                .header("X-Refresh-Token", refresh).POST(HttpRequest.BodyPublishers.noBody()).build(),
                                HttpResponse.BodyHandlers.ofString());
                assertEquals(401, refreshAttempt.statusCode());
        }

        @Test
        void concurrentReviewsAreUniqueAndAggregateRatingsStayConsistent() throws Exception {
                Trip t = trip(5);
                var firstBooking = call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=1", null, traveler);
                assertEquals(200, call("POST", "/api/payments/mock/bookings/" + id(firstBooking, "bookingId"), null, traveler).statusCode());
                var secondBooking = call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=1", null, secondTraveler);
                assertEquals(200, call("POST", "/api/payments/mock/bookings/" + id(secondBooking, "bookingId"), null, secondTraveler).statusCode());
                t.setStatus(Trip.TripStatus.COMPLETED);
                trips.save(t);
                String content = "{\"tripId\":" + t.getTripId() + ",\"rating\":4,\"comment\":\"Ottimo\"}";
                try (var pool = Executors.newFixedThreadPool(4)) {
                        var jobs = new ArrayList<Future<HttpResponse<String>>>();
                        for (int i = 0; i < 4; i++)
                                jobs.add(pool.submit(() -> call("POST", "/api/reviews", content, traveler)));
                        int accepted = 0;
                        for (var job : jobs) {
                                int status = job.get().statusCode();
                                if (status == 200)
                                        accepted++;
                                else
                                        assertEquals(400, status);
                        }
                        assertEquals(1, accepted);
                }
                var second = call("POST", "/api/reviews", content.replace("\"rating\":4", "\"rating\":2"),
                                secondTraveler);
                assertEquals(200, second.statusCode(), second.body());
                assertEquals(0, new BigDecimal("3")
                                .compareTo(trips.findById(t.getTripId()).orElseThrow().getAverageRating()));
                assertEquals(404, call("PUT", "/api/reviews/999999", "{\"rating\":5}", traveler).statusCode());
        }

        @Test
        void organizerResponsesRejectEmptyContentAndConcurrentDuplicates() throws Exception {
                Trip t = trip(5);
                var booked = call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=1", null, traveler);
                assertEquals(200, call("POST", "/api/payments/mock/bookings/" + id(booked, "bookingId"), null, traveler).statusCode());
                t.setStatus(Trip.TripStatus.COMPLETED);
                trips.save(t);
                long review = id(call("POST", "/api/reviews", "{\"tripId\":" + t.getTripId() + ",\"rating\":5}",
                                traveler), "reviewId");
                String empty = "{\"reviewId\":" + review + ",\"comment\":\" \"}";
                assertEquals(400, call("POST", "/api/responses", empty, owner).statusCode());
                String content = empty.replace(" ", "Grazie");
                assertEquals(403, call("POST", "/api/responses", content, otherOrganizer).statusCode());
                try (var pool = Executors.newFixedThreadPool(3)) {
                        var jobs = new ArrayList<Future<HttpResponse<String>>>();
                        for (int i = 0; i < 3; i++)
                                jobs.add(pool.submit(() -> call("POST", "/api/responses", content, owner)));
                        int accepted = 0;
                        long response = 0;
                        for (var job : jobs) {
                                var result = job.get();
                                if (result.statusCode() == 201) {
                                        accepted++;
                                        response = id(result, "id");
                                } else
                                        assertEquals(409, result.statusCode());
                        }
                        assertEquals(1, accepted);
                        assertEquals(400, call("PUT", "/api/responses/" + response, empty, owner).statusCode());
                }
        }

        @Test
        void draftAndDeletedPhotoGalleriesRespectOfferVisibility() throws Exception {
                Trip t = trip(3);
                t.setStatus(Trip.TripStatus.DRAFT);
                trips.save(t);
                String path = "/api/trip-images/trip/" + t.getTripId();
                assertEquals(404, call("GET", path, null, null).statusCode());
                assertEquals(404, call("GET", path, null, traveler).statusCode());
                assertEquals(200, call("GET", path, null, owner).statusCode());
                assertEquals(404, call("GET", "/api/trip-images/trip/999999", null, null).statusCode());
                assertEquals(404, call("GET", "/api/activity-images/activity/999999", null, null).statusCode());
        }

        @Test
        void concurrentRegistrationCannotCreateCaseVariantsOfTheSameIdentity() throws Exception {
                String name = "case_" + UUID.randomUUID().toString().substring(0, 8);
                String body = "{\"username\":\"" + name + "\",\"email\":\"" + name
                                + "@example.test\",\"fullName\":\"Case Test\",\"password\":\"TestPassword!42\",\"role\":\"TRAVELER\"}";
                try (var pool = Executors.newFixedThreadPool(2)) {
                        var first = pool.submit(() -> call("POST", "/api/users", body, null));
                        var second = pool.submit(() -> call("POST", "/api/users",
                                        body.replace(name, name.toUpperCase(Locale.ROOT)), null));
                        var statuses = List.of(first.get().statusCode(), second.get().statusCode());
                        assertEquals(1, statuses.stream().filter(code -> code == 201).count());
                        assertEquals(1, statuses.stream().filter(code -> code == 409).count());
                        assertEquals(name, users.findByUsernameIgnoreCase(name).orElseThrow().getUsername());
                }
        }
        @Test
        void paymentUnlocksReviewWithoutWaitingForDepartureAndDuplicateReviewIsRejected() throws Exception {
                Trip t = trip(6);
                String review = "{\"tripId\":" + t.getTripId() + ",\"rating\":5,\"comment\":\"Prenotazione confermata\"}";
                var reserved = call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=2", null, traveler);
                assertEquals(200, reserved.statusCode(), reserved.body());
                long booking = id(reserved, "bookingId");
                assertEquals(400, call("POST", "/api/reviews", review, traveler).statusCode());
                assertEquals(200, call("POST", "/api/payments/mock/bookings/" + booking, null, traveler).statusCode());
                assertEquals(200, call("POST", "/api/payments/mock/bookings/" + booking, null, traveler).statusCode());
                assertEquals(4, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
                assertEquals(409, call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=1", null, traveler).statusCode());
                var created = call("POST", "/api/reviews", review, traveler);
                assertEquals(200, created.statusCode(), created.body());
                assertEquals(400, call("POST", "/api/reviews", review, traveler).statusCode());
                assertEquals(204, call("DELETE", "/api/bookings/" + booking, null, traveler).statusCode());
                assertEquals(6, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
                assertEquals(400, call("POST", "/api/reviews", review, traveler).statusCode());
        }
        @Test
        void activityDuplicatesAreRejectedAndCancellationAllowsRebooking() throws Exception {
                Activity a = new Activity(); a.setCreatedBy(owner); a.setTitle("Museo"); a.setCity("Roma");
                a.setCategory(Activity.Category.VISIT); a.setPrice(10d); a.setMaxSeats(20); a.setAvailableSeats(20);
                a.setStartDate(LocalDateTime.now().plusDays(2)); a.setEndDate(LocalDateTime.now().plusDays(2).plusHours(2));
                a.setStatus(Activity.Status.PUBLISHED); a = activities.save(a);
                String path = "/api/bookings/activities/" + a.getActivityId() + "?seats=2";
                var first = call("POST", path, null, traveler); assertEquals(200, first.statusCode(), first.body());
                assertEquals(409, call("POST", path, null, traveler).statusCode());
                assertEquals(18, activities.findById(a.getActivityId()).orElseThrow().getAvailableSeats());
                assertEquals(204, call("DELETE", "/api/bookings/" + id(first, "bookingId"), null, traveler).statusCode());
                assertEquals(20, activities.findById(a.getActivityId()).orElseThrow().getAvailableSeats());
                assertEquals(200, call("POST", path, null, traveler).statusCode());
                assertEquals(18, activities.findById(a.getActivityId()).orElseThrow().getAvailableSeats());
        }

}
