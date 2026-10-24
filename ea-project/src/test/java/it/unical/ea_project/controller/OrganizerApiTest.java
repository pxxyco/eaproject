package it.unical.ea_project.controller;

import it.unical.ea_project.domain.User;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrganizerApiTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired it.unical.ea_project.repository.TripRepository trips;
    @Autowired it.unical.ea_project.repository.TripImageRepository images;
    private final JsonMapper json = JsonMapper.builder().build();
    private User user(String name, User.Role role) {
        User user = new User(); user.setUsername(name); user.setEmail(name + "@example.test"); user.setRole(role);
        return users.saveAndFlush(user);
    }
    private String token(User user) { return "Bearer " + jwt.generateAccessToken(user); }
    private final String offer = """
        {"title":"Tour","destinationCountry":"Italia","destinationCity":"Roma",
         "startDate":"2027-06-01","endDate":"2027-06-03","totalPrice":25,
         "maxSeats":10,"availableSeats":999,"status":"PUBLISHED",
         "stages":[{"title":"Museo","category":"VISIT","day":1,"orderInDay":1}]}
        """;
    private long create(User owner) throws Exception {
        var result = mvc.perform(post("/api/trips").header("Authorization", token(owner))
                .contentType(MediaType.APPLICATION_JSON).content(offer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(10))
                .andExpect(jsonPath("$.stages[0].title").value("Museo")).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("tripId").asLong();
    }
    @Test void organizerDataAndMutationsAreIsolatedByOwnerAndRole() throws Exception {
        User owner = user("api-owner", User.Role.ORGANIZER);
        User other = user("api-other", User.Role.ORGANIZER);
        User traveler = user("api-traveler", User.Role.TRAVELER);
        long id = create(owner);
        mvc.perform(get("/api/trips/organizer/" + owner.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/trips/organizer/" + owner.getId()).header("Authorization", token(other))).andExpect(status().isForbidden());
        mvc.perform(get("/api/trips/organizer/" + owner.getId()).header("Authorization", token(owner))).andExpect(status().isOk()).andExpect(jsonPath("$[0].tripId").value(id));
        mvc.perform(post("/api/trips").header("Authorization", token(traveler)).contentType(MediaType.APPLICATION_JSON).content(offer)).andExpect(status().isForbidden());
        mvc.perform(put("/api/trips/" + id).header("Authorization", token(other)).contentType(MediaType.APPLICATION_JSON).content(offer)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/trips/" + id).header("Authorization", token(other))).andExpect(status().isForbidden());
        mvc.perform(post("/api/trips/" + id + "/stages").header("Authorization", token(other)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Tappa\",\"category\":\"VISIT\",\"day\":1,\"orderInDay\":2}")).andExpect(status().isForbidden());
    }
    @Test void travelerReservationIsVisibleOnOrganizerDashboardAndCapacityUpdate() throws Exception {
        User owner = user("api-owner", User.Role.ORGANIZER);
        User traveler = user("api-traveler", User.Role.TRAVELER);
        long id = create(owner);
        mvc.perform(post("/api/bookings/trips/" + id).param("seats", "2").header("Authorization", token(traveler)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itemTitle").value("Tour"));
        mvc.perform(get("/api/bookings/organizer/mine").header("Authorization", token(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].seats").value(2));
        mvc.perform(put("/api/trips/" + id).header("Authorization", token(owner)).contentType(MediaType.APPLICATION_JSON)
                .content(offer.replace("\"maxSeats\":10", "\"maxSeats\":12").replace(",\"stages\":[{\"title\":\"Museo\",\"category\":\"VISIT\",\"day\":1,\"orderInDay\":1}]", "")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableSeats").value(10));
        mvc.perform(get("/api/payments/organizer/revenue").header("Authorization", token(owner))).andExpect(status().isOk()).andExpect(content().string("0"));
        mvc.perform(delete("/api/trips/" + id).header("Authorization", token(owner))).andExpect(status().isNoContent());
        mvc.perform(get("/api/trips/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/bookings/organizer/mine").header("Authorization", token(owner))).andExpect(status().isOk()).andExpect(jsonPath("$[0].seats").value(2));
    }
    @Test void onlyOwnerCanDeletePhotosAndCoverIsReassigned() throws Exception {
        User owner = user("api-owner", User.Role.ORGANIZER);
        User other = user("api-other", User.Role.ORGANIZER);
        long id = create(owner);
        var trip = trips.findById(id).orElseThrow();
        var cover = images.saveAndFlush(new it.unical.ea_project.domain.TripImage(null, trip, "/photo/cover.jpg", 1));
        images.saveAndFlush(new it.unical.ea_project.domain.TripImage(null, trip, "/photo/second.jpg", 2));
        trip.setCoverPhotoUrl(cover.getImageUrl()); trips.saveAndFlush(trip);
        mvc.perform(delete("/api/trip-images/" + cover.getImageId()).header("Authorization", token(other))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/trip-images/" + cover.getImageId()).header("Authorization", token(owner))).andExpect(status().isNoContent());
        mvc.perform(get("/api/trips/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.coverPhotoUrl").value("/photo/second.jpg"));
    }
    @Test void uploadedPhotoIsServedFromConfiguredStorage() throws Exception {
        User owner = user("api-owner", User.Role.ORGANIZER);
        long id = create(owner);
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        var file = new org.springframework.mock.web.MockMultipartFile("files", "cover.png", "image/png", output.toByteArray());
        var response = mvc.perform(multipart("/api/trip-images/trip/" + id + "/photos").file(file).header("Authorization", token(owner)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String url = json.readTree(response).get(0).get("imageUrl").asText();
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"));
        mvc.perform(get("/api/trips/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.coverPhotoUrl").value(url));
    }
    @Test void malformedOfferIsRejectedBeforePersistence() throws Exception {
        User owner = user("api-owner", User.Role.ORGANIZER);
        mvc.perform(post("/api/trips").header("Authorization", token(owner)).contentType(MediaType.APPLICATION_JSON)
                .content(offer.replace("\"totalPrice\":25", "\"totalPrice\":-1"))).andExpect(status().isBadRequest());
    }
}
