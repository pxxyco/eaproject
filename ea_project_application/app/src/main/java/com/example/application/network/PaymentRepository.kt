package com.example.application.network

import java.math.BigDecimal

internal data class PaymentReceipt(val title: String, val amount: BigDecimal, val reference: String, val paidAt: String)
internal object PaymentRepository {
    suspend fun pay(id: Long): PaymentReceipt = ApiService.post("/api/payments/mock/bookings/$id", null, PaymentReceipt::class.java).requireData()
}

