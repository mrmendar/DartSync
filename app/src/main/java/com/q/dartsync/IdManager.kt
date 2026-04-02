package com.q.dartsync

import android.content.Context
import java.util.UUID

object IdManager {
    private const val PREF_NAME = "UserPrefs"
    private const val KEY_GUEST_ID = "guest_unique_id"

    fun getGuestId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var guestId = prefs.getString(KEY_GUEST_ID, null)

        if (guestId == null) {
            // Eğer yoksa, yeni bir rastgele UUID üret (Örn: 550e8400-e29b-41d4-a716-446655440000)
            guestId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_GUEST_ID, guestId).apply()
        }

        return guestId
    }
}