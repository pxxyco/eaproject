package it.unical.ea_project.service.impl;

import it.unical.ea_project.repository.ActivityImageRepository;
import it.unical.ea_project.repository.ActivityRepository;
import it.unical.ea_project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityServiceDateSearchTest {

    private final ActivityRepository repository = mock(ActivityRepository.class);
    private final ActivityServiceImpl service = new ActivityServiceImpl(repository,
            mock(ActivityImageRepository.class), mock(UserRepository.class));

    @Test
    void selectedDayUsesWholeDayInterval() {
        LocalDate day = LocalDate.of(2026, 9, 28);
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime nextDayStart = day.plusDays(1).atStartOfDay();
        when(repository.search(eq(""), isNull(), eq(dayStart), eq(nextDayStart), any(LocalDateTime.class), eq(PageRequest.of(0, 9))))
                .thenReturn(List.of());

        service.searchActivityHomeDtos("", null, day, 0, 9);

        verify(repository).search(eq(""), isNull(), eq(dayStart), eq(nextDayStart), any(LocalDateTime.class), eq(PageRequest.of(0, 9)));
    }

    @Test
    void absentDateDoesNotFilter() {
        when(repository.searchWithoutDate(eq("test"), isNull(), any(LocalDateTime.class), eq(PageRequest.of(1, 9))))
                .thenReturn(List.of());

        service.searchActivityHomeDtos(" test ", null, null, 1, 9);

        verify(repository).searchWithoutDate(eq("test"), isNull(), any(LocalDateTime.class), eq(PageRequest.of(1, 9)));
    }
}