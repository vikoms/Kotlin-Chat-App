package com.example.kotlinchatapp.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService

class MyFirebaseInstanceId: FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            val refreshToken = instanceIdResult.token
            if (firebaseUser != null) {
                updateToken(refreshToken)
            }
        }
    }

    private fun updateToken(refreshToken: String) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val ref = FirebaseDatabase.getInstance().getReference("Tokens")
        val token = Token(refreshToken)
        ref.child(firebaseUser!!.uid).setValue(token)
    }
}