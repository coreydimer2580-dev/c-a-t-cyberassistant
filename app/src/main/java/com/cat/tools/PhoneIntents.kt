package com.cat.tools

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Hands a normal number to the phone's own dialer or SMS app.
 * ACTION_DIAL does not place the call. ACTION_SENDTO does not send the text.
 */
object PhoneIntents {
    fun dialIntent(number: String): Intent? {
        val parsed = AuPhone.parse(number) ?: return null
        return Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", parsed, null)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun smsIntent(number: String, body: String?): Intent? {
        val parsed = AuPhone.parse(number) ?: return null
        return Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", parsed, null)).apply {
            if (!body.isNullOrBlank()) {
                putExtra("sms_body", body.take(1000))
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun launch(context: Context, intent: Intent?): String? {
        if (intent == null) return "That is not a normal phone number."
        return try {
            context.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "No dialer or SMS app found on this phone."
        }
    }
}
