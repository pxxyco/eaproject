package com.example.application.network

import com.example.application.dto.UserDTO
import com.example.application.device.CardExpiry
import java.time.YearMonth
import java.util.Locale

internal data class ProfileInput(val username: String, val email: String, val fullName: String) {
    fun normalized() = copy(username = username.trim().lowercase(Locale.ROOT),
        email = email.trim().lowercase(Locale.ROOT), fullName = fullName.trim().replace(Regex("\\s+"), " "))
    fun error(): String? = when {
        !Regex("[a-zA-Z0-9._-]{3,30}").matches(username.trim()) -> "Username: usa 3–30 lettere, numeri, punti, trattini o underscore."
        email.trim().length > 254 || !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email.trim()) -> "Inserisci un indirizzo email valido."
        fullName.trim().isEmpty() || fullName.trim().length > 120 -> "Inserisci un nome completo, massimo 120 caratteri."
        else -> null
    }
}
internal data class AccountPreferences(val chatAlerts: Boolean = true, val bookingAlerts: Boolean = true, val personalized: Boolean = true)
internal data class SavedCard(val id: Long, val brand: String, val last4: String, val holder: String, val expiryMonth: Int, val expiryYear: Int)
internal data class CardInput(val brand: String, val last4: String, val holder: String, val expiryMonth: Int, val expiryYear: Int) {
    fun normalized() = copy(expiryYear = CardExpiry.fourDigitYear(expiryYear))
    fun error(now: YearMonth = YearMonth.now()): String? {
        if (brand !in listOf("Visa", "Mastercard", "Amex", "Altro")) return "Seleziona il circuito."
        if (!last4.matches(Regex("[0-9]{4}"))) return "Inserisci solo le ultime quattro cifre."
        if (holder.isBlank() || holder.length > 120) return "Inserisci un intestatario valido."
        val date = runCatching { YearMonth.of(normalized().expiryYear, expiryMonth) }.getOrNull()
        return CardExpiry.error(date, now)
    }
}
internal data class SupportTicket(val id: Long, val category: String, val subject: String, val message: String, val status: String, val createdAt: String?)
internal data class TicketInput(val category: String, val subject: String, val message: String) {
    fun error(): String? = when {
        category.isBlank() || category.length > 120 -> "Seleziona una categoria."
        subject.isBlank() || subject.length > 180 -> "Inserisci un oggetto, massimo 180 caratteri."
        message.isBlank() || message.length > 4000 -> "Inserisci un messaggio, massimo 4000 caratteri."
        else -> null
    }
}

internal interface AccountGateway {
    suspend fun saveProfile(input: ProfileInput): ApiResult<UserDTO>
    suspend fun removePhoto(): ApiResult<UserDTO>
    suspend fun uploadPhoto(bytes: ByteArray, mime: String): ApiResult<UserDTO>
    suspend fun preferences(): ApiResult<AccountPreferences>
    suspend fun savePreferences(input: AccountPreferences): ApiResult<AccountPreferences>
    suspend fun cards(): ApiResult<Array<SavedCard>>
    suspend fun addCard(input: CardInput): ApiResult<SavedCard>
    suspend fun removeCard(id: Long): ApiResult<Unit>
    suspend fun tickets(page: Int): ApiResult<Array<SupportTicket>>
    suspend fun sendTicket(input: TicketInput): ApiResult<SupportTicket>
    suspend fun changePassword(current: String, new: String): ApiResult<Unit>
    suspend fun revokeSessions(): ApiResult<Unit>
}

internal object AccountRepository : AccountGateway {
    override suspend fun saveProfile(input: ProfileInput) = ApiService.put("/api/users/me", input.normalized(), UserDTO::class.java)
    override suspend fun removePhoto() = ApiService.delete("/api/users/me/profile-photo", UserDTO::class.java)
    override suspend fun uploadPhoto(bytes: ByteArray, mime: String) = ApiService.uploadPhoto("/api/users/me/profile-photo", "file", bytes, mime, UserDTO::class.java)
    override suspend fun preferences() = ApiService.get("/api/account/settings", AccountPreferences::class.java)
    override suspend fun savePreferences(input: AccountPreferences) = ApiService.put("/api/account/settings", input, AccountPreferences::class.java).also {
        if (it is ApiResult.Success) RecommendationRepository.invalidate()
    }
    override suspend fun cards() = ApiService.get("/api/account/cards", Array<SavedCard>::class.java)
    override suspend fun addCard(input: CardInput) = ApiService.post("/api/account/cards", input.normalized(), SavedCard::class.java)
    override suspend fun removeCard(id: Long) = ApiService.delete("/api/account/cards/$id", Unit::class.java)
    override suspend fun tickets(page: Int) = ApiService.get("/api/account/tickets?page=$page", Array<SupportTicket>::class.java)
    override suspend fun sendTicket(input: TicketInput) = ApiService.post("/api/account/tickets", input, SupportTicket::class.java)
    override suspend fun changePassword(current: String, new: String) = ApiService.post("/api/users/change-password",
        mapOf("currentPassword" to current, "newPassword" to new), Unit::class.java)
    override suspend fun revokeSessions() = ApiService.post("/api/users/logout", null, Unit::class.java)
}

internal fun ApiResult<*>.accountError(): String = when (this) {
    is ApiResult.HttpError -> {
        val detail = runCatching { ApiService.gson.fromJson(message, com.google.gson.JsonObject::class.java)
            .get("message")?.asString }.getOrNull()
        detail?.takeIf { it.isNotBlank() } ?: when (code) {
            400 -> "Controlla i dati inseriti."
            401 -> "Sessione scaduta. Accedi nuovamente."
            403 -> "Non hai i permessi per questa operazione."
            409 -> "Username o email già utilizzati."
            else -> "Operazione non riuscita. Riprova."
        }
    }
    is ApiResult.RateLimited -> "Riprova tra ${retryAfterSeconds} secondi."
    is ApiResult.NetworkError -> "Connessione non disponibile. Riprova."
    is ApiResult.Success -> ""
}
