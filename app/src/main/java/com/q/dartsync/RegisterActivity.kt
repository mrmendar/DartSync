package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        btnSignUp.setOnClickListener {
            val nickname = findViewById<EditText>(R.id.etRegNickname).text.toString().trim().lowercase()
            val email = findViewById<EditText>(R.id.etRegEmail).text.toString().trim()
            val pass = findViewById<EditText>(R.id.etRegPassword).text.toString().trim()

            if (nickname.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm boşlukları doldur!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "İsim kontrol ediliyor...", Toast.LENGTH_SHORT).show()

            // 1. ADIM: Kullanıcı adı kontrolü
            db.collection("usernames").document(nickname).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        Toast.makeText(this, "Bu isim alınmış!", Toast.LENGTH_SHORT).show()
                    } else {
                        // 2. ADIM: Auth ile kullanıcı oluşturma
                        Toast.makeText(this, "Hesap oluşturuluyor...", Toast.LENGTH_SHORT).show()
                        auth.createUserWithEmailAndPassword(email, pass)
                            .addOnSuccessListener {
                                val uid = auth.currentUser!!.uid

                                // 3. ADIM: Firestore'a toplu kayıt (Batch)
                                val batch = db.batch()
                                val userRef = db.collection("users").document(uid)
                                val nameRef = db.collection("usernames").document(nickname)

                                batch.set(userRef, hashMapOf("nickname" to nickname, "uid" to uid, "email" to email))
                                batch.set(nameRef, hashMapOf("uid" to uid))

                                batch.commit()
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Kayıt Başarılı!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, HomeActivity::class.java))
                                        finishAffinity()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Veri Kayıt Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Auth Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // EĞER BURASI ÇALIŞIYORSA: İnternet veya Firestore izin hatası vardır.
                    Toast.makeText(this, "Veritabanı bağlantı hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("DART_ERROR", "Firestore error", e)
                }
        }
    }
}