package com.q.dartsync

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Uygulama ayarlarını yerel hafızada tutmak için SharedPreferences
    private val prefs by lazy { getSharedPreferences("DartSyncPrefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()

        setupPasswordSettings()
        setupExperienceSettings()
    }

    /**
     * 🔐 Şifre Değiştirme Ayarları
     * Firebase üzerinden e-posta sıfırlama bağlantısı gönderir.
     */
    private fun setupPasswordSettings() {
        findViewById<CardView>(R.id.cardChangePassword).setOnClickListener {
            val userEmail = auth.currentUser?.email

            if (userEmail != null) {
                AlertDialog.Builder(this)
                    .setTitle("Güvenlik Doğrulaması")
                    .setMessage("Şifrenizi yenilemek için $userEmail adresine bir sıfırlama bağlantısı gönderilecektir. Devam edilsin mi?")
                    .setPositiveButton("Bağlantıyı Gönder") { _, _ ->
                        sendPasswordReset(userEmail)
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            } else {
                Toast.makeText(this, "E-posta adresi bulunamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Sıfırlama e-postası başarıyla gönderildi.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hata oluştu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * 🎮 Oyun Deneyimi Ayarları
     * Ses ve Titreşim tercihlerini SharedPreferences'a kaydeder.
     */
    private fun setupExperienceSettings() {
        val switchVibrate = findViewById<SwitchCompat>(R.id.switchVibration)
        val switchSound = findViewById<SwitchCompat>(R.id.switchSound)

        // 1. Mevcut ayarları yerel hafızadan yükle (Varsayılan: true)
        switchVibrate.isChecked = prefs.getBoolean("vibration_enabled", true)
        switchSound.isChecked = prefs.getBoolean("sound_enabled", true)

        // 2. Titreşim ayarını dinle ve kaydet
        switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
            if (isChecked) {
                Toast.makeText(this, "Titreşim aktif", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Ses ayarını dinle ve kaydet
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }
    }
}