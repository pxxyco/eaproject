package it.unical.ea_project.repository.account;

import it.unical.ea_project.domain.account.AccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSettingsRepository extends JpaRepository<AccountSettings, Long> {
}
