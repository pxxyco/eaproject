package com.example.application.device

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.github.devnied.emvnfccard.exception.CommunicationException
import com.github.devnied.emvnfccard.parser.EmvTemplate
import com.github.devnied.emvnfccard.parser.IProvider
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.atomic.AtomicReference

/** Bounded, read-only EMV exchange. No transaction history, PIN operations or APDU logging. */
internal class NfcCardReader {
    private val connection = AtomicReference<IsoDep?>()
    @Volatile private var closed = true
    fun start() { closed = false }
    fun close() { closed = true; runCatching { connection.getAndSet(null)?.close() } }
    fun read(tag: Tag): ScannedCard {
        if (closed) throw IOException("Lettura terminata")
        val tech = IsoDep.get(tag) ?: throw IOException("Carta non compatibile")
        connection.set(tech)
        try {
            if (closed) throw IOException("Lettura terminata")
            tech.connect()
            if (closed) throw IOException("Lettura terminata")
            tech.timeout = 1500
            val deadline = System.nanoTime() + 15_000_000_000L
            var exchanges = 0
            val provider = object : IProvider {
                override fun transceive(command: ByteArray): ByteArray {
                    if (++exchanges > 120 || System.nanoTime() > deadline || connection.get() !== tech || closed)
                        throw CommunicationException("Lettura terminata")
                    return try { tech.transceive(command) } catch (_: IOException) { throw CommunicationException("Carta rimossa") }
                }
                override fun getAt(): ByteArray = byteArrayOf()
            }
            val config = EmvTemplate.Config().setContactLess(true).setReadAllAids(false)
                .setReadTransactions(false).setReadAt(false).setReadCplc(false)
                .setReadAllRecords(false).setReadExtendedData(false)
            val card = EmvTemplate.Builder().setProvider(provider).setConfig(config).build().readEmvCard()
            val pan = card.cardNumber?.filter(Char::isDigit).orEmpty()
            if (pan.length !in 13..19) throw IOException("Dati carta non disponibili")
            val expiry = card.expireDate?.let { Calendar.getInstance().apply { time = it } }
            val holder = listOfNotNull(card.holderFirstname, card.holderLastname).filter(String::isNotBlank).joinToString(" ").takeIf(String::isNotBlank)
            return ScannedCard(CardNumbers.brand(pan) ?: "Altro", pan.takeLast(4), holder,
                expiry?.get(Calendar.MONTH)?.plus(1), expiry?.get(Calendar.YEAR))
        } finally { connection.compareAndSet(tech, null); runCatching { tech.close() } }
    }
}
