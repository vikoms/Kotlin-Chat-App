package com.example.kotlinchatapp

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.kotlinchatapp.entity.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_visit_user.*

class VisitUserActivity : AppCompatActivity() {


    companion object {
        const val USER_ID = "USER_ID"
        internal val TAG = VisitUserActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visit_user)

        val uid = intent.getStringExtra(USER_ID)
        updateUI(uid)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        btn_send_message.setOnClickListener{
            val intent =
                Intent(this@VisitUserActivity, MessageChatActivity::class.java).apply {
                    putExtra(MessageChatActivity.VISIT_ID,uid )
                }
            startActivity(intent)
        }

    }

    private fun updateUI(uid: String?) {
        val ref = FirebaseDatabase.getInstance().reference.child("users").child(uid!!)
        ref.addListenerForSingleValueEvent(object  : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.d(TAG,p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val user = dataSnapshot.getValue(User::class.java)
                    Glide.with(this@VisitUserActivity).load(user?.profileImageUrl)
                        .into(img_profile)
                    tv_username.text = user?.username
                    tv_status.text = user?.status
                    if (user?.status.equals("online")) {
                        tv_status.setTextColor(Color.parseColor("#05df29"))
                    } else {
                        tv_status.setTextColor(ContextCompat.getColor(this@VisitUserActivity,android.R.color.darker_gray))
                    }
                }
            }

        })
    }
}
