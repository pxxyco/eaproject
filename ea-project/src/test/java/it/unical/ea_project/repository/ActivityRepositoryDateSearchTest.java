package it.unical.ea_project.repository;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase(replace = org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class ActivityRepositoryDateSearchTest {

    @Autowired private ActivityRepository repository;
    @Autowired private UserRepository userRepository;

    @Test
    void selectedDateIncludesOverlappingActivitiesOnly() {
        User organizer = new User();
        organizer.setUsername("organizer-date-test");
        organizer.setEmail("organizer-date-test@example.com");
        organizer = userRepository.save(organizer);

        LocalDate date = LocalDate.of(2026, 9, 28);
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime nextDayStart = date.plusDays(1).atStartOfDay();
        saveActivity(organizer, "starts-at-midnight", dayStart, nextDayStart, false);
        saveActivity(organizer, "ends-at-midnight", dayStart.minusDays(1), dayStart, false);
        saveActivity(organizer, "spans-day", dayStart.minusDays(1), nextDayStart.plusDays(1), false);
        saveActivity(organizer, "starts-tomorrow", nextDayStart, nextDayStart.plusHours(1), false);
        saveActivity(organizer, "ended-yesterday", dayStart.minusDays(1), dayStart.minusSeconds(1), false);
        saveActivity(organizer, "deleted", dayStart, nextDayStart, true);

        List<String> matching = repository.search("", null, dayStart, nextDayStart, PageRequest.of(0, 9))
                .stream().map(Activity::getTitle).sorted().toList();
        assertEquals(List.of("ends-at-midnight", "spans-day", "starts-at-midnight"), matching);

        assertEquals(5, repository.search("", null, null, null, PageRequest.of(0, 9)).size());
    }

    private void saveActivity(User organizer, String title, LocalDateTime start, LocalDateTime end, boolean deleted) {
        Activity activity = new Activity();
        activity.setCreatedBy(organizer);
        activity.setTitle(title);
        activity.setCategory(Activity.Category.EXCURSION);
        activity.setStatus(Activity.Status.PUBLISHED);
        activity.setStartDate(start);
        activity.setEndDate(end);
        if (deleted) {
            activity.setDeletedAt(LocalDateTime.now());
        }
        repository.save(activity);
    }
}