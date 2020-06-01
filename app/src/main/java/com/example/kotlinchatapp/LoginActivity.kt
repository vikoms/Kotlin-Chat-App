package com.example.kotlinchatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btn_login.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val email = edt_email.text.toString()
        val password = edt_password.text.toString()
        if (password.isEmpty() || email.isEmpty()) {
            Toast.makeText(this@LoginActivity,"Please enter email or password", Toast.LENGTH_SHORT).show()
            return
        }
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this@LoginActivity,"Login success", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity,MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity,"Login failed ${task.exception}", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
            }.addOnFailureListener {
                Log.d("login exception","${it.message}")
                Toast.makeText(this@LoginActivity,"Login failed ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
