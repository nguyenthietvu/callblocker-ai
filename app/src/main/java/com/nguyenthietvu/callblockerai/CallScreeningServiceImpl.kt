package com.nguyenthietvu.callblockerai

import android.telecom.Call
import android.telecom.CallScreeningService

class CallScreeningServiceImpl : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val incomingNumber = callDetails.handle?.let { uri ->
            if (uri.scheme == "tel") uri.schemeSpecificPart else uri.toString()
        }?.filter(Char::isDigit).orEmpty()

        val blockedNumbers = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .getStringSet(BLOCKED_NUMBERS, emptySet())
            .orEmpty()
        val blocked = incomingNumber.isNotEmpty() && incomingNumber in blockedNumbers

        val response = if (blocked) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        } else {
            CallResponse.Builder().build()
        }
        respondToCall(callDetails, response)
    }

    companion object {
        const val PREFERENCES_NAME = "call_blocker_preferences"
        const val BLOCKED_NUMBERS = "blocked_numbers"
    }
}
