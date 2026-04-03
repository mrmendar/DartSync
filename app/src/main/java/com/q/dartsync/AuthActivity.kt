package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.BuildConfig
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        FirebaseAuth.getInstance().signOut()
        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etLoginEmail)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)
        val tvGuestLogin = findViewById<TextView>(R.id.tvGuestLogin)

        // 1. GİRİŞ YAP BUTONU
        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        goToHome()
                    } else {
                        Toast.makeText(this, "Giriş Başarısız: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Lütfen e-posta ve şifre gir imat!", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. KAYIT OL SAYFASINA GİT
        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // 3. ÜYE OLMADAN DEVAM ET (MİSAFİR GİRİŞİ)
        tvGuestLogin.setOnClickListener {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Toast.makeText(this, "Misafir olarak giriş yapıldı!", Toast.LENGTH_SHORT).show()
                    goToHome()
                }
                .addOnFailureListener { e ->
                    // ⚠️ Burası çalışmıyorsa Firebase Console'da "Anonymous" kapalıdır!
                    Toast.makeText(this, "Misafir girişi hatası: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // 4. OTURUM KONTROLÜ (Uygulama açıldığında kullanıcı zaten giriş yapmış mı?)
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser

        // Eğer halihazırda giriş yapmış bir kullanıcı varsa
        if (currentUser != null) {
            // Uygulama "Debug" modundaysa (yani sen Android Studio'dan Build alıyorsan)
            if (BuildConfig.DEBUG) {
                // Geliştirme aşamasında her seferinde giriş ekranını görmek için oturumu kapatıyoruz
                auth.signOut()
                // Not: Burada herhangi bir işlem yapmıyoruz, uygulama AuthActivity'de kalıyor.
            } else {
                // Uygulama yayınlanmış sürümdeyse (kullanıcıların telefonunda)
                // Kullanıcıyı bekletmeden ana ekrana gönderiyoruz
                goToHome()
            }
        }
    }

    // Ortak yönlendirme fonksiyonu
    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Giriş ekranını tamamen kapat
    }
}