package it.unical.ea_project_javafx.util;

public enum AppErrorCode {

    ERR_NETWORK_UNREACHABLE("ERR-101", "Impossibile raggiungere il server."),
    ERR_API_TIMEOUT("ERR-102", "Il server ha impiegato troppo tempo a rispondere."),
    ERR_SERVICE_MAINTENANCE("ERR-103", "I servizi sono temporaneamente in manutenzione."),
    ERR_RATE_LIMITED("ERR-104", "Troppe richieste in poco tempo."),
    ERR_AUTH_EXPIRED("ERR-201", "Sessione scaduta. Effettua nuovamente il login."),
    ERR_UNAUTHORIZED("ERR-202", "Non disponi dei permessi per accedere a questa risorsa."),
    ERR_UNKNOWN("ERR-500", "Si è verificato un errore inatteso.");

    private final String code;
    private final String defaultDescription;

    AppErrorCode(String code, String defaultDescription) {
        this.code = code;
        this.defaultDescription = defaultDescription;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultDescription() {
        return defaultDescription;
    }

}