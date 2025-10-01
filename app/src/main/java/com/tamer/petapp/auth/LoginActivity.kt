package com.tamer.petapp.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.tamer.petapp.MainActivity
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "onCreate başladı")
            
            // View binding
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Firebase başlatma
            FirebaseApp.initializeApp(this)
            auth = FirebaseAuth.getInstance()
            Log.d(TAG, "Firebase Auth başlatıldı")
            
            // Firebase bağlantı testi
            testFirebaseConnection()

            // Kullanıcı zaten giriş yapmış mı kontrol et
            if (auth.currentUser != null) {
                Log.d(TAG, "Kullanıcı zaten giriş yapmış: ${auth.currentUser?.email}")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            }

            setupListeners()
            
            Log.d(TAG, "onCreate tamamlandı")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate hatası: ${e.message}", e)
            Toast.makeText(this, "Bir hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun testFirebaseConnection() {
        try {
            // Firebase Database bağlantı testi
            val database = FirebaseDatabase.getInstance()
            val testRef = database.getReference("test")
            
            testRef.setValue("test_${System.currentTimeMillis()}")
                .addOnSuccessListener {
                    Log.d(TAG, "Firebase Database bağlantısı başarılı")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firebase Database bağlantısı başarısız: ${e.message}")
                    Toast.makeText(this, "Firebase Database bağlantısı başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
                
            // Firebase Auth bağlantı testi
            auth.fetchSignInMethodsForEmail("test@example.com")
                .addOnSuccessListener {
                    Log.d(TAG, "Firebase Auth bağlantısı başarılı")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firebase Auth bağlantısı başarısız: ${e.message}")
                    Toast.makeText(this, "Firebase Auth bağlantısı başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase bağlantı testi hatası: ${e.message}")
            Toast.makeText(this, "Firebase bağlantı testi hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        try {
            // Kayıt ol butonuna tıklandığında
            binding.tvRegister.setOnClickListener {
                Log.d(TAG, "Kayıt ol butonuna tıklandı")
                startActivity(Intent(this, RegisterActivity::class.java))
            }

            // Giriş yap butonuna tıklandığında
            binding.btnLogin.setOnClickListener {
                Log.d(TAG, "Giriş yap butonuna tıklandı")
                if (validateInputs()) {
                    loginUser()
                }
            }
            
            // Test giriş butonuna tıklandığında
            binding.btnTestLogin.setOnClickListener {
                Log.d(TAG, "Test giriş butonuna tıklandı")
                testLogin()
            }

            // Şifremi unuttum butonuna tıklandığında
            binding.tvForgotPassword.setOnClickListener {
                Log.d(TAG, "Şifremi unuttum butonuna tıklandı")
                showForgotPasswordDialog()
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupListeners hatası: ${e.message}", e)
        }
    }
    
    private fun testLogin() {
        try {
            // Test için sabit değerler
            val testEmail = "test@example.com"
            val testPassword = "123456"
            
            Log.d(TAG, "Test giriş yapılıyor: $testEmail")
            Toast.makeText(this, "Test giriş başlatılıyor: $testEmail", Toast.LENGTH_SHORT).show()
            
            // Yükleniyor göstergesi
            binding.progressBar.visibility = View.VISIBLE
            
            // Firebase ile giriş
            auth.signInWithEmailAndPassword(testEmail, testPassword)
                .addOnCompleteListener(this) { task ->
                    binding.progressBar.visibility = View.GONE
                    
                    if (task.isSuccessful) {
                        // Başarılı giriş
                        Log.d(TAG, "Test giriş başarılı: ${auth.currentUser?.email}")
                        Toast.makeText(this, "Test giriş başarılı", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // Başarısız giriş
                        val errorMessage = task.exception?.message ?: "Bilinmeyen hata"
                        Log.e(TAG, "Test giriş başarısız: $errorMessage")
                        Toast.makeText(this, "Test giriş başarısız: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e(TAG, "testLogin hatası: ${e.message}", e)
            Toast.makeText(this, "Test giriş hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun validateInputs(): Boolean {
        try {
            var isValid = true

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

            Log.d(TAG, "Girişler doğrulandı: $isValid")
            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "validateInputs hatası: ${e.message}", e)
            return false
        }
    }

    private fun loginUser() {
        try {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            
            Log.d(TAG, "Giriş yapılıyor: $email")

            // Yükleniyor göstergesi
            binding.progressBar.visibility = View.VISIBLE
            
            // Firebase ile giriş
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    binding.progressBar.visibility = View.GONE
                    
                    if (task.isSuccessful) {
                        // Başarılı giriş
                        Log.d(TAG, "Giriş başarılı: ${auth.currentUser?.email}")
                        Toast.makeText(this, getString(R.string.welcome), Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // Başarısız giriş
                        val errorMessage = task.exception?.message ?: "Bilinmeyen hata"
                        Log.e(TAG, "Giriş başarısız: $errorMessage")
                        Toast.makeText(this, "Giriş başarısız: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e(TAG, "loginUser hatası: ${e.message}", e)
            Toast.makeText(this, "Giriş yapılırken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showForgotPasswordDialog() {
        try {
            // Dialog için custom layout oluştur
            val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
            val tilEmail = dialogView.findViewById<TextInputLayout>(R.id.tilEmail)
            val etEmail = dialogView.findViewById<TextInputEditText>(R.id.etEmail)

            // Mevcut e-posta adresini doldur
            etEmail.setText(binding.etEmail.text.toString())

            // Dialog oluştur
            AlertDialog.Builder(this)
                .setTitle("Şifre Sıfırlama")
                .setView(dialogView)
                .setPositiveButton("Sıfırlama Linki Gönder") { dialog, _ ->
                    val email = etEmail.text.toString().trim()
                    if (email.isEmpty()) {
                        tilEmail.error = "E-posta adresi gerekli"
                        return@setPositiveButton
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        tilEmail.error = "Geçerli bir e-posta adresi girin"
                        return@setPositiveButton
                    }
                    sendPasswordResetEmail(email)
                    dialog.dismiss()
                }
                .setNegativeButton("İptal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "showForgotPasswordDialog hatası: ${e.message}", e)
            Toast.makeText(this, "Şifre sıfırlama dialog hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        try {
            binding.progressBar.visibility = View.VISIBLE
            
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    binding.progressBar.visibility = View.GONE
                    
                    if (task.isSuccessful) {
                        Log.d(TAG, "Şifre sıfırlama e-postası gönderildi: $email")
                        Toast.makeText(
                            this,
                            "Şifre sıfırlama linki e-posta adresinize gönderildi",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val errorMessage = task.exception?.message ?: "Bilinmeyen hata"
                        Log.e(TAG, "Şifre sıfırlama e-postası gönderilemedi: $errorMessage")
                        Toast.makeText(
                            this,
                            "Şifre sıfırlama e-postası gönderilemedi: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e(TAG, "sendPasswordResetEmail hatası: ${e.message}", e)
            Toast.makeText(this, "Şifre sıfırlama hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
} 