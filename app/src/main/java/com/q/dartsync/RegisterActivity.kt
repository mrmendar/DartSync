package com.q.dartsync

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
            val nickname = findViewById<EditText>(R.id.etRegNickname).text.toString().trim()
            val email = findViewById<EditText>(R.id.etRegEmail).text.toString().trim()
            val pass = findViewById<EditText>(R.id.etRegPassword).text.toString().trim()

            // Boşluk kontrolü
            if (nickname.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldur!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Arama kolaylığı için küçük harf versiyonu
            val searchName = nickname.lowercase()

            Toast.makeText(this, "İsim kontrol ediliyor...", Toast.LENGTH_SHORT).show()

            // 1. ADIM: Kullanıcı adı kontrolü (usernames koleksiyonundan)
            db.collection("usernames").document(searchName).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        Toast.makeText(this, "Bu isim zaten alınmış!", Toast.LENGTH_SHORT).show()
                    } else {
                        // 2. ADIM: Firebase Auth ile kullanıcı oluşturma
                        Toast.makeText(this, "Hesap oluşturuluyor...", Toast.LENGTH_SHORT).show()
                        auth.createUserWithEmailAndPassword(email, pass)
                            .addOnSuccessListener {
                                val uid = auth.currentUser!!.uid

                                // 3. ADIM: Firestore'a toplu kayıt (Batch)
                                // Bir mühendis olarak verinin tutarlılığı için Batch kullanman harika!
                                val batch = db.batch()

                                // 'users' dokümanı: Kullanıcının tüm sosyal ve oyun verileri
                                val userRef = db.collection("users").document(uid)

                                // 'usernames' dokümanı: İsim çakışmasını önlemek için kilit
                                val nameRef = db.collection("usernames").document(searchName)

                                val userData = hashMapOf(
                                    "uid" to uid,
                                    "nickname" to nickname, // Orijinal hali (Örn: Tuna)
                                    "searchName" to searchName, // Arama için küçük harf (Örn: tuna)
                                    "email" to email,
                                    "targetLevel" to 120, // Finish Master başlangıç seviyesi
                                    "createdAt" to FieldValue.serverTimestamp(), // Kayıt tarihi
                                    "friendsCount" to 0 // Başlangıçta arkadaş sayısı
                                )

                                batch.set(userRef, userData)
                                batch.set(nameRef, hashMapOf("uid" to uid))

                                batch.commit()
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Kayıt Başarılı!", Toast.LENGTH_SHORT).show()
                                        // HomeActivity'ye yönlendirme
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
                    Toast.makeText(this, "Bağlantı hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("DART_ERROR", "Firestore error", e)
                }
        }
    }
}