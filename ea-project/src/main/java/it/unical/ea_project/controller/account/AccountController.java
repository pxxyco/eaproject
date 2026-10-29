package it.unical.ea_project.controller.account;

import it.unical.ea_project.domain.account.AccountSettings;
import it.unical.ea_project.domain.payment.SavedCard;
import it.unical.ea_project.domain.support.SupportTicket;
import it.unical.ea_project.repository.account.AccountSettingsRepository;
import it.unical.ea_project.repository.payment.SavedCardRepository;
import it.unical.ea_project.repository.support.SupportTicketRepository;
import it.unical.ea_project.security.AuthUser;
import java.math.BigInteger;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountSettingsRepository settings;
    private final SavedCardRepository cards;
    private final SupportTicketRepository tickets;

    public record Preferences(boolean chatAlerts, boolean bookingAlerts, boolean personalized, String brand,
            String bio) {
    }

    public record CardInput(String brand, String last4, String holder, int expiryMonth, int expiryYear) {
    }

    public record Payout(String holder, String iban, String bic) {
    }

    public record TicketInput(String category, String subject, String message) {
    }

    @GetMapping("/settings")
    public AccountSettings get(@AuthenticationPrincipal AuthUser user) {
        return settings.findById(user.id()).orElseGet(() -> new AccountSettings(user.id()));
    }

    @PutMapping("/settings")
    @Transactional
    public AccountSettings save(@AuthenticationPrincipal AuthUser user, @RequestBody Preferences input) {
        var s = get(user);
        s.setChatAlerts(input.chatAlerts());
        s.setBookingAlerts(input.bookingAlerts());
        s.setPersonalized(input.personalized());
        if ("ORGANIZER".equals(user.role())) {
            s.setBrand(text(input.brand(), 120, false));
            s.setBio(text(input.bio(), 2000, false));
        }
        return settings.save(s);
    }

    @GetMapping("/cards")
    public List<SavedCard> cards(@AuthenticationPrincipal AuthUser user) {
        traveler(user);
        return cards.findByUserIdOrderByIdDesc(user.id());
    }

    @PostMapping("/cards")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SavedCard add(@AuthenticationPrincipal AuthUser user, @RequestBody CardInput input) {
        traveler(user);
        if (cards.countByUserId(user.id()) >= 10)
            throw new IllegalArgumentException("Puoi salvare al massimo 10 carte dimostrative.");
        if (input.last4() == null || !input.last4().matches("[0-9]{4}")
                || !Set.of("Visa", "Mastercard", "Amex", "Altro").contains(input.brand() == null ? "" : input.brand()))
            throw new IllegalArgumentException("Dati della carta non validi.");
        int expiryYear = input.expiryYear() >= 0 && input.expiryYear() <= 99 ? 2000 + input.expiryYear() : input.expiryYear();
        try {
            if (YearMonth.of(expiryYear, input.expiryMonth()).isBefore(YearMonth.now())
                    || expiryYear > YearMonth.now().getYear() + 20)
                throw new IllegalArgumentException();
        } catch (Exception ex) {
            throw new IllegalArgumentException("La scadenza della carta non è valida.");
        }
        var c = new SavedCard();
        c.setUserId(user.id());
        c.setBrand(input.brand());
        c.setLast4(input.last4());
        c.setHolder(text(input.holder(), 120, true));
        c.setExpiryMonth(input.expiryMonth());
        c.setExpiryYear(expiryYear);
        return cards.save(c);
    }

    @DeleteMapping("/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remove(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        cards.delete(cards.findByIdAndUserId(id, user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carta non trovata.")));
    }

    @PutMapping("/payout")
    @Transactional
    public AccountSettings payout(@AuthenticationPrincipal AuthUser user, @RequestBody Payout input) {
        if (!"ORGANIZER".equals(user.role()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        String iban = input.iban() == null ? "" : input.iban().replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        if (!iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}") || !validIban(iban))
            throw new IllegalArgumentException("IBAN non valido.");
        String bic = input.bic() == null ? "" : input.bic().strip().toUpperCase(Locale.ROOT);
        if (!bic.isEmpty() && !bic.matches("[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?"))
            throw new IllegalArgumentException("BIC non valido.");
        var s = get(user);
        s.setPayoutHolder(text(input.holder(), 120, true));
        s.setIban(iban);
        s.setBic(bic);
        return settings.save(s);
    }

    @GetMapping("/tickets")
    public List<SupportTicket> tickets(@AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page) {
        return tickets.findByUserIdOrderByIdDesc(user.id(), PageRequest.of(Math.max(0, Math.min(10000, page)), 20));
    }

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicket ticket(@AuthenticationPrincipal AuthUser user, @RequestBody TicketInput input) {
        var t = new SupportTicket();
        t.setUserId(user.id());
        t.setCategory(text(input.category(), 120, true));
        t.setSubject(text(input.subject(), 180, true));
        t.setMessage(text(input.message(), 4000, true));
        return tickets.save(t);
    }

    private void traveler(AuthUser u) {
        if (!"TRAVELER".equals(u.role()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private String text(String value, int max, boolean required) {
        String s = value == null ? "" : value.strip();
        if (s.length() > max || required && s.isBlank())
            throw new IllegalArgumentException("Controlla i campi obbligatori e la lunghezza del testo.");
        return s;
    }

    private boolean validIban(String value) {
        String rotated = value.substring(4) + value.substring(0, 4);
        StringBuilder digits = new StringBuilder();
        rotated.chars().forEach(c -> {
            if (Character.isDigit(c))
                digits.append((char) c);
            else
                digits.append(c - 'A' + 10);
        });
        return new BigInteger(digits.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
