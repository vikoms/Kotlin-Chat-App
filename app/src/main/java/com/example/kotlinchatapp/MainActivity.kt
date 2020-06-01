package com.example.kotlinchatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.bumptech.glide.Glide
import com.example.kotlinchatapp.entity.Chat
import com.example.kotlinchatapp.entity.User
import com.example.kotlinchatapp.fragments.ChatFragment
import com.example.kotlinchatapp.fragments.SearchFragment
import com.example.kotlinchatapp.fragments.SettingFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    private lateinit var refUsers: DatabaseReference
    private var mUser: FirebaseUser? = null

    companion object {
        internal val TAG = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

//      get data to display the total unread messages
        val ref = FirebaseDatabase.getInstance().reference.child("Chats")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.d(TAG, p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var countUnreadMessage = 0

                val sectionPagerAdapter = SectionPagerAdapter(supportFragmentManager)
                for (snapshot in dataSnapshot.children) {
                    val chat = snapshot.getValue(Chat::class.java)
                    if (chat?.receiver.equals(mUser?.uid) && !chat?.isseen!!) {
                        countUnreadMessage += 1
                    }
                }

                if (countUnreadMessage == 0) {
                    sectionPagerAdapter.addFragment(ChatFragment(), "Chats")
                } else {
                    sectionPagerAdapter.addFragment(ChatFragment(), "($countUnreadMessage) Chats")
                }

                sectionPagerAdapter.addFragment(SearchFragment(), "Search")
                sectionPagerAdapter.addFragment(SettingFragment(), "Setting")
                view_pager.adapter = sectionPagerAdapter
                tabs.setupWithViewPager(view_pager)
            }

        })

//      get data for toolbar
        mUser = FirebaseAuth.getInstance().currentUser
        refUsers = FirebaseDatabase.getInstance().getReference("users").child(mUser!!.uid)
        refUsers.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val user: User? = dataSnapshot.getValue(
                        User::class.java
                    )
                    tv_username.text = user?.username
                    Glide.with(this@MainActivity).load(user?.profileImageUrl)
                        .into(img_profile)
                }
            }

        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Logout")
                .setMessage("Are you sure?")
                .setNeutralButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }
                .setNegativeButton("No") { dialog, which ->
                    dialog.dismiss()
                }
                .setPositiveButton("Yes") { dialog, which ->
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this@MainActivity, RegisterActivity::class.java))
                    finish()
                }.show()
        }

        return true
    }

    private fun updateStatus(status: String) {
        val ref = FirebaseDatabase.getInstance().reference.child("users").child(mUser!!.uid)
        val hashMap = HashMap<String, Any>()
        hashMap["status"] = status

        ref?.updateChildren(hashMap)

    }


    override fun onResume() {
        super.onResume()
        updateStatus("online")
    }


    override fun onPause() {
        super.onPause()
        updateStatus("offline")
    }

}
