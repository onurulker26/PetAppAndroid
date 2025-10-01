package com.tamer.petapp.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.MainActivity
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ActivityRegisterBinding
import com.tamer.petapp.model.User

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "onCreate başladı")
            
            // View binding
            binding = ActivityRegisterBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Firebase başlatma
            FirebaseApp.initializeApp(this)
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firebase başlatıldı")

            setupListeners()
            
            Log.d(TAG, "onCreate tamamlandı")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate hatası: ${e.message}", e)
        }
    }

    private fun setupListeners() {
        try {
            // Giriş yap butonuna tıklandığında
            binding.tvLogin.setOnClickListener {
                Log.d(TAG, "Giriş yap butonuna tıklandı")
                finish() // Giriş ekranına geri dön
            }

            // Kayıt ol butonuna tıklandığında
            binding.btnRegister.setOnClickListener {
                Log.d(TAG, "Kayıt ol butonuna tıklandı")
                if (validateInputs()) {
                    registerUser()
                }
            }
            
            // Test kayıt butonu
            binding.btnTestRegister.setOnClickListener {
                Log.d(TAG, "Test kayıt butonuna tıklandı")
                testRegister()
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupListeners hatası: ${e.message}", e)
        }
    }
    
    private fun testRegister() {
        try {
            // Test için sabit değerler
            val testEmail = "test${System.currentTimeMillis()}@example.com"
            val testPassword = "123456"
            
            Log.d(TAG, "Test kayıt yapılıyor: $testEmail")
            
            // Yükleniyor göstergesi
            binding.progressBar.visibility = View.VISIBLE
            binding.btnTestRegister.isEnabled = false
            
            // Sadece Authentication ile kayıt
            auth.createUserWithEmailAndPassword(testEmail, testPassword)
                .addOnCompleteListener(this) { task ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnTestRegister.isEnabled = true
                    
                    if (task.isSuccessful) {
                        Log.d(TAG, "Test kayıt başarılı: ${auth.currentUser?.uid}")
                        
                        // Ana ekrana yönlendir
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        val errorMessage = task.exception?.message ?: "Bilinmeyen hata"
                        Log.e(TAG, "Test kayıt başarısız: $errorMessage")
                    }
                }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.btnTestRegister.isEnabled = true
            Log.e(TAG, "testRegister hatası: ${e.message}", e)
        }
    }

    private fun validateInputs(): Boolean {
        try {
            var isValid = true

            // Ad Soyad doğrulama
            val name = binding.etName.text.toString().trim()
            if (TextUtils.isEmpty(name)) {
                binding.tilName.error = getString(R.string.field_required)
                isValid = false
            } else {
                binding.tilName.error = null
            }

            // E-posta doğrulama
            val email = binding.etEmail.text.toString().trim()
            if (TextUtils.isEmpty(email)) {
                binding.tilEmail.error = getString(R.string.field_required)
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = getString(R.string.invalid_email)
                isValid = false
            } else {
                binding.tilEmail.error = null
            }

            // Şifre doğrulama
            val password = binding.etPassword.text.toString()
            if (TextUtils.isEmpty(password)) {
                binding.tilPassword.error = getString(R.string.field_required)
                isValid = false
            } else if (password.length < 6) {
                binding.tilPassword.error = getString(R.string.password_too_short)
                isValid = false
            } else {
                binding.tilPassword.error = null
            }

            // Şifre onay doğrulama
            val confirmPassword = binding.etConfirmPassword.text.toString()
            if (TextUtils.isEmpty(confirmPassword)) {
                binding.tilConfirmPassword.error = getString(R.string.field_required)
                isValid = false
            } else if (password != confirmPassword) {
                binding.tilConfirmPassword.error = getString(R.string.passwords_not_match)
                isValid = false
            } else {
                binding.tilConfirmPassword.error = null
            }

            Log.d(TAG, "Girişler doğrulandı: $isValid")
            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "validateInputs hatası: ${e.message}", e)
            return false
        }
    }

    private fun registerUser() {
        try {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            
            Log.d(TAG, "Kayıt yapılıyor: $email")

            // UI'ı güncelle
            binding.apply {
                progressBar.visibility = View.VISIBLE
                btnRegister.isEnabled = false
                btnTestRegister.isEnabled = false
                tvLogin.isEnabled = false
            }

            // Firebase Authentication ile kullanıcı oluştur
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { authTask ->
                    if (authTask.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            // Kullanıcı profil bilgilerini güncelle
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()

                            auth.currentUser?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        // Firestore'a kullanıcı bilgilerini kaydet
                                        val user = hashMapOf(
                                            "userId" to userId,
                                            "name" to name,
                                            "email" to email,
                                            "createdAt" to com.google.firebase.Timestamp.now()
                                        )

                                        firestore.collection("users")
                                            .document(userId)
                                            .set(user)
                                            .addOnSuccessListener {
                                                Log.d(TAG, "Kullanıcı Firestore'a kaydedildi")

                                                // UI'ı güncelle
                                                binding.apply {
                                                    progressBar.visibility = View.GONE
                                                    btnRegister.isEnabled = true
                                                    btnTestRegister.isEnabled = true
                                                    tvLogin.isEnabled = true
                                                }

                                                // Ana ekrana yönlendir
                                                startActivity(Intent(this, MainActivity::class.java)
                                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
                                                finishAffinity()
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e(TAG, "Firestore kayıt hatası: ${e.message}")
                                                // Firestore hatası durumunda bile kullanıcı kaydı tamamlandı sayılır
                                                handleRegistrationError()
                                            }
                                    } else {
                                        // Profil güncelleme hatası
                                        Log.e(TAG, "Profil güncelleme hatası: ${profileTask.exception?.message}")
                                        handleRegistrationError()
                                    }
                                }
                        }
                    } else {
                        // Authentication hatası
                        val errorMessage = authTask.exception?.message ?: "Bilinmeyen hata"
                        Log.e(TAG, "Authentication hatası: $errorMessage")
                        handleRegistrationError()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "registerUser hatası: ${e.message}", e)
            handleRegistrationError()
        }
    }

    private fun handleRegistrationError() {
        // UI'ı güncelle
        binding.apply {
            progressBar.visibility = View.GONE
            btnRegister.isEnabled = true
            btnTestRegister.isEnabled = true
            tvLogin.isEnabled = true
        }
    }
} 