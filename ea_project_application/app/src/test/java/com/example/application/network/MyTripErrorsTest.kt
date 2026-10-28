package com.example.application.network

import org.junit.Assert.*
import org.junit.Test

class MyTripErrorsTest {
    @Test fun errorsNeverExposeEndpointOrServerDetails() {
        listOf(400, 401, 403, 404, 409, 500).forEach {
            assertFalse(myTripHttpError(it).contains("/api/"))
            assertFalse(myTripHttpError(it).contains(it.toString()))
        }
    }
    @Test fun expiredSessionAndDeniedPermissionHaveDifferentMessages() {
        assertTrue(myTripHttpError(401).contains("accedi"))
        assertTrue(myTripHttpError(403).contains("permessi"))
    }
}
