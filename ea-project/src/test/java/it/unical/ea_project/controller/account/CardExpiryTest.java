package it.unical.ea_project.controller.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.unical.ea_project.repository.account.AccountSettingsRepository;
import it.unical.ea_project.repository.payment.SavedCardRepository;
import it.unical.ea_project.repository.support.SupportTicketRepository;
import it.unical.ea_project.security.AuthUser;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class CardExpiryTest {
    private final SavedCardRepository cards = mock(SavedCardRepository.class);
    private final AccountController controller = new AccountController(mock(AccountSettingsRepository.class), cards, mock(SupportTicketRepository.class));
    private final AuthUser user = new AuthUser(7L, "test@example.it", "TRAVELER");

    @Test void shortAndFullYearsAreStoredIdenticallyAndCurrentMonthRemainsValid() {
        when(cards.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var now = YearMonth.now();
        var shortCard = controller.add(user, input(now.getMonthValue(), now.getYear() % 100));
        var fullCard = controller.add(user, input(now.getMonthValue(), now.getYear()));
        assertEquals(now.getYear(), shortCard.getExpiryYear());
        assertEquals(fullCard.getExpiryYear(), shortCard.getExpiryYear());
    }

    @Test void rejectsActuallyExpiredAndMalformedDatesWithoutWritingThem() {
        var past = YearMonth.now().minusMonths(1);
        assertThrows(IllegalArgumentException.class, () -> controller.add(user, input(past.getMonthValue(), past.getYear())));
        assertThrows(IllegalArgumentException.class, () -> controller.add(user, input(13, YearMonth.now().getYear() + 1)));
        verify(cards, never()).save(any());
    }
    private AccountController.CardInput input(int month, int year) {
        return new AccountController.CardInput("Visa", "1111", "Mario Rossi", month, year);
    }
}
