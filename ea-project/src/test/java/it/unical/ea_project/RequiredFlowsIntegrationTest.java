package it.unical.ea_project;

import it.unical.ea_project.domain.*;
import it.unical.ea_project.repository.*;
import it.unical.ea_project.service.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequiredFlowsIntegrationTest {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired ActivityRepository activities;
    @Autowired BookingRepository bookings;
    @Autowired TripImageRepository tripImages;
    @Autowired ActivityImageRepository activityImages;
    @Autowired NotificationRepository notifications;
    @Autowired JwtService jwt;
    final HttpClient http = HttpClient.newHttpClient();
    User owner, otherOrganizer, traveler, secondTraveler;

    @BeforeEach void setup() {
        owner = user(User.Role.ORGANIZER); otherOrganizer = user(User.Role.ORGANIZER);
        traveler = user(User.Role.TRAVELER); secondTraveler = user(User.Role.TRAVELER);
    }
    User user(User.Role role) {
        User u = new User(); String name = UUID.randomUUID().toString();
        u.setUsername(name); u.setEmail(name + "@example.test"); u.setFullName(name); u.setRole(role);
        return users.save(u);
    }
    Trip trip(int seats) {
        Trip t = new Trip(); t.setCreatedBy(owner); t.setTitle("Viaggio di prova");
        t.setDestinationCity("Roma"); t.setStartDate(LocalDate.now().plusDays(3)); t.setEndDate(LocalDate.now().plusDays(4));
        t.setTotalPrice(BigDecimal.TEN); t.setMaxSeats(seats); t.setAvailableSeats(seats); t.setStatus(Trip.TripStatus.PUBLISHED);
        return trips.save(t);
    }
    HttpResponse<String> call(String method, String path, String body, User user) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(java.time.Duration.ofSeconds(15));
        if (user != null) b.header("Authorization", "Bearer " + jwt.generateAccessToken(user));
        b.header("Content-Type", "application/json").method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
    long id(HttpResponse<String> response, String field) {
        var m = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*(\\d+)").matcher(response.body());
        assertTrue(m.find(), response.body()); return Long.parseLong(m.group(1));
    }
    @Test void protectedEndpointsRejectAnonymousAndForeignOwners() throws Exception {
        Trip t = trip(3);
        assertEquals(401, call("GET", "/api/bookings/mine", null, null).statusCode());
        String stage = "{\"title\":\"Tappa\",\"day\":1,\"orderInDay\":1,\"category\":\"VISIT\"}";
        assertEquals(403, call("POST", "/api/trips/" + t.getTripId() + "/stages", stage, otherOrganizer).statusCode());
        assertEquals(200, call("POST", "/api/trips/" + t.getTripId() + "/stages", stage, owner).statusCode());
        assertEquals(403, call("GET", "/api/trips/organizer/" + owner.getId(), null, otherOrganizer).statusCode());
        TripImage image = tripImages.save(new TripImage(null, t, "/photo/test.jpg", 1));
        assertEquals(403, call("DELETE", "/api/trip-images/" + image.getImageId(), null, otherOrganizer).statusCode());
        assertTrue(tripImages.existsById(image.getImageId()));
        assertEquals(204, call("DELETE", "/api/trip-images/" + image.getImageId(), null, owner).statusCode());
    }
    @Test void privateVisibilityRevokesSharesAndPendingInvitations() throws Exception {
        var created = call("POST", "/api/favorite-lists", "{\"name\":\"Lista\",\"visibility\":\"SHARED\"}", traveler);
        assertEquals(201, created.statusCode(), created.body()); long listId = id(created, "id");
        String invite = "{\"listId\":" + listId + ",\"identifier\":\"" + secondTraveler.getUsername() + "\",\"permission\":\"VIEW\"}";
        assertEquals(201, call("POST", "/api/list-sharings", invite, traveler).statusCode());
        var notification = notifications.findByUserAndTypeOrderByCreatedAtDesc(secondTraveler, Notification.NotificationType.LIST_INVITATION).getFirst();
        assertEquals(200, call("POST", "/api/notifications/" + notification.getNotificationId() + "/accept", null, secondTraveler).statusCode());
        assertEquals(200, call("GET", "/api/list-trips/list/" + listId, null, secondTraveler).statusCode());
        assertEquals(200, call("PUT", "/api/favorite-lists/" + listId, "{\"name\":\"Lista\",\"visibility\":\"PRIVATE\"}", traveler).statusCode());
        assertEquals(403, call("GET", "/api/list-trips/list/" + listId, null, secondTraveler).statusCode());
        assertEquals(400, call("POST", "/api/list-sharings", invite, traveler).statusCode());
        assertEquals(200, call("PUT", "/api/favorite-lists/" + listId, "{\"name\":\"Lista\",\"visibility\":\"SHARED\"}", traveler).statusCode());
        assertEquals(403, call("GET", "/api/list-trips/list/" + listId, null, secondTraveler).statusCode());
        assertEquals(201, call("POST", "/api/list-sharings", invite, traveler).statusCode());
        var pending = notifications.findByUserAndTypeOrderByCreatedAtDesc(secondTraveler, Notification.NotificationType.LIST_INVITATION).getFirst();
        assertEquals(200, call("PUT", "/api/favorite-lists/" + listId, "{\"name\":\"Lista\",\"visibility\":\"PRIVATE\"}", traveler).statusCode());
        assertEquals(400, call("POST", "/api/notifications/" + pending.getNotificationId() + "/accept", null, secondTraveler).statusCode());
    }
    @Test void bookingIsConfirmedAndCancellationRestoresSeatsOnce() throws Exception {
        Trip t = trip(2); String path = "/api/bookings/trips/" + t.getTripId() + "?seats=1";
        var booked = call("POST", path, null, traveler);
        assertEquals(200, booked.statusCode(), booked.body()); assertTrue(booked.body().contains("CONFIRMED"));
        long bookingId = id(booked, "bookingId");
        assertEquals(403, call("DELETE", "/api/bookings/" + bookingId, null, secondTraveler).statusCode());
        assertEquals(204, call("DELETE", "/api/bookings/" + bookingId, null, traveler).statusCode());
        assertEquals(204, call("DELETE", "/api/bookings/" + bookingId, null, traveler).statusCode());
        assertEquals(2, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
    }
    @Test void lastSeatCannotBeSoldTwice() throws Exception {
        Trip t = trip(1); String path = "/api/bookings/trips/" + t.getTripId() + "?seats=1";
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var first = executor.submit(() -> { start.await(); return call("POST", path, null, traveler).statusCode(); });
            var second = executor.submit(() -> { start.await(); return call("POST", path, null, secondTraveler).statusCode(); });
            start.countDown();
            assertEquals(List.of(200, 400), java.util.stream.Stream.of(first.get(), second.get()).sorted().toList());
        }
        assertEquals(0, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
    }
    @Test void cancelledOrPastTripsCannotBeBooked() throws Exception {
        Trip t = trip(2); t.setStatus(Trip.TripStatus.CANCELLED); trips.save(t);
        assertEquals(400, call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=1", null, traveler).statusCode());
        t.setStatus(Trip.TripStatus.PUBLISHED); t.setStartDate(LocalDate.now().minusDays(1)); trips.save(t);
        assertEquals(400, call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=1", null, traveler).statusCode());
    }
    @Test void activityReservationAndImageOwnershipUseSameRules() throws Exception {
        Activity a = new Activity(); a.setCreatedBy(owner); a.setTitle("Visita"); a.setCategory(Activity.Category.VISIT);
        a.setCity("Roma"); a.setStartDate(LocalDateTime.now().plusDays(2)); a.setEndDate(LocalDateTime.now().plusDays(2).plusHours(1));
        a.setPrice(12d); a.setMaxSeats(1); a.setAvailableSeats(1); a.setStatus(Activity.Status.PUBLISHED); a = activities.save(a);
        ActivityImage image = activityImages.save(new ActivityImage(null, a, "/photo/test.jpg", 1));
        assertEquals(403, call("DELETE", "/api/activity-images/" + image.getImageId(), null, otherOrganizer).statusCode());
        assertEquals(204, call("DELETE", "/api/activity-images/" + image.getImageId(), null, owner).statusCode());
        var booked = call("POST", "/api/bookings/activities/" + a.getActivityId() + "?seats=1", null, traveler);
        assertEquals(200, booked.statusCode(), booked.body()); assertTrue(booked.body().contains("CONFIRMED"));
        assertEquals(400, call("POST", "/api/bookings/activities/" + a.getActivityId() + "?seats=1", null, secondTraveler).statusCode());
    }
    @Test void organizerCrudPreservesReservedSeatsAndItinerary() throws Exception {
        Trip t = trip(3);
        assertEquals(200, call("POST", "/api/bookings/trips/" + t.getTripId() + "?seats=2", null, traveler).statusCode());
        String update = "{\"title\":\"Viaggio aggiornato\",\"destinationCity\":\"Roma\",\"startDate\":\"" + t.getStartDate()
                + "\",\"endDate\":\"" + t.getEndDate() + "\",\"totalPrice\":10,\"maxSeats\":4,\"availableSeats\":999,\"status\":\"PUBLISHED\"}";
        assertEquals(403, call("PUT", "/api/trips/" + t.getTripId(), update, otherOrganizer).statusCode());
        var response = call("PUT", "/api/trips/" + t.getTripId(), update, owner);
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(2, trips.findById(t.getTripId()).orElseThrow().getAvailableSeats());
        assertEquals(400, call("PUT", "/api/trips/" + t.getTripId(), update.replace("\"maxSeats\":4", "\"maxSeats\":1"), owner).statusCode());
        assertEquals(200, call("POST", "/api/trips/" + t.getTripId() + "/stages", "{\"title\":\"Museo\",\"day\":1,\"orderInDay\":1,\"category\":\"VISIT\"}", owner).statusCode());
        var detail = call("GET", "/api/trips/" + t.getTripId(), null, null);
        assertEquals(200, detail.statusCode()); assertTrue(detail.body().contains("Museo"), detail.body());
        assertEquals(200, call("GET", "/api/trips/organizer/" + owner.getId(), null, owner).statusCode());
        assertEquals(403, call("DELETE", "/api/trips/" + t.getTripId(), null, otherOrganizer).statusCode());
        assertEquals(204, call("DELETE", "/api/trips/" + t.getTripId(), null, owner).statusCode());
        assertEquals(404, call("GET", "/api/trips/" + t.getTripId(), null, null).statusCode());
        assertEquals(Trip.TripStatus.CANCELLED, trips.findById(t.getTripId()).orElseThrow().getStatus());
        assertTrue(call("GET", "/api/bookings/mine", null, traveler).body().contains("\"itemStatus\":\"CANCELLED\""));
    }
    @Test void registrationLoginRefreshAndIdentityAreConnected() throws Exception {
        String name = "mobile_" + UUID.randomUUID().toString().substring(0, 8);
        String registration = "{\"username\":\"" + name + "\",\"email\":\"" + name + "@example.test\",\"fullName\":\"Mobile Test\",\"password\":\"TestPassword!42\",\"role\":\"TRAVELER\"}";
        var registered = call("POST", "/api/users", registration, null);
        assertEquals(201, registered.statusCode(), registered.body());
        var login = call("POST", "/api/users/login", "{\"identifier\":\"" + name + "\",\"password\":\"TestPassword!42\"}", null);
        assertEquals(200, login.statusCode());
        String access = login.headers().firstValue("Authorization").orElseThrow();
        String refresh = login.headers().firstValue("X-Refresh-Token").orElseThrow();
        var me = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/users/me")).header("Authorization", access).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, me.statusCode()); assertTrue(me.body().contains(name));
        var renewed = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/users/refresh")).header("X-Refresh-Token", refresh).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, renewed.statusCode()); assertTrue(renewed.headers().firstValue("Authorization").orElseThrow().startsWith("Bearer "));
        String profile = "{\"username\":\"" + name + "\",\"email\":\"updated_" + name + "@example.test\",\"fullName\":\"Updated User\"}";
        var updated = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/users/me"))
                .header("Authorization", access).header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(profile)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, updated.statusCode(), updated.body()); assertTrue(updated.body().contains("Updated User"));
        var legacy = call("POST", "/api/users", registration.replace(name, name + "b").replace("\"role\":", "\"oauthProvider\":false,\"role\":"), null);
        assertEquals(201, legacy.statusCode(), legacy.body());
    }

    @Test void mobileCanCreateOffersWithoutOptionalCollections() throws Exception {
        String start = LocalDate.now().plusDays(4).toString();
        String end = LocalDate.now().plusDays(5).toString();
        String tripBody = "{\"title\":\"Offerta mobile\",\"destinationCity\":\"Roma\",\"destinationCountry\":\"Italia\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end + "\",\"totalPrice\":10,\"maxSeats\":3,\"status\":\"PUBLISHED\"}";
        var createdTrip = call("POST", "/api/trips", tripBody, owner);
        assertEquals(200, createdTrip.statusCode(), createdTrip.body());
        assertTrue(createdTrip.body().contains("\"stages\":[]"), createdTrip.body());
        String activityBody = "{\"title\":\"Attività mobile\",\"city\":\"Roma\",\"category\":\"VISIT\",\"startDate\":\"" + start + "T10:00:00\",\"endDate\":\"" + start + "T11:00:00\",\"price\":10,\"maxSeats\":3}";
        var createdActivity = call("POST", "/api/activities", activityBody, owner);
        assertEquals(201, createdActivity.statusCode(), createdActivity.body());
        assertEquals(200, call("GET", "/api/activities/organizer/mine", null, owner).statusCode());
        assertEquals(403, call("DELETE", "/api/activities/" + id(createdActivity, "activityId"), null, otherOrganizer).statusCode());
        assertEquals(204, call("DELETE", "/api/activities/" + id(createdActivity, "activityId"), null, owner).statusCode());
    }

}
