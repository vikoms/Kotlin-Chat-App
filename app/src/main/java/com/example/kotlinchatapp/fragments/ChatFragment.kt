package com.example.kotlinchatapp.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.kotlinchatapp.R
import com.example.kotlinchatapp.adapter.UserAdapter
import com.example.kotlinchatapp.entity.ChatList
import com.example.kotlinchatapp.entity.User
import com.example.kotlinchatapp.notifications.Token
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.fragment_chat.*

/**
 * A simple [Fragment] subclass.
 */
class ChatFragment : Fragment() {

    private lateinit var userAdapter: UserAdapter
    private lateinit var users: List<User>
    private lateinit var userChatList: List<ChatList>
    private lateinit var firebaseUser: FirebaseUser
    companion object {
        internal val TAG = ChatFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            val refreshToken = instanceIdResult.token
            updateToken(refreshToken )
        }

        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (activity != null) {
            with(rv_chatlist) {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(activity)
            }

            firebaseUser = FirebaseAuth.getInstance().currentUser!!

            userChatList = ArrayList()
            val ref = FirebaseDatabase.getInstance().reference.child("ChatList").child(firebaseUser.uid)
            ref.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    Log.d(TAG,p0.message)
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    (userChatList as ArrayList).clear()
                    for (snapshot in dataSnapshot.children) {
                        val chatList = snapshot.getValue(ChatList::class.java)
                        (userChatList as ArrayList).add(chatList!!)
                    }
                    retrieveChatList()
                }

            })



        }

    }

    private fun updateToken(refreshToken: String) {
        val ref = FirebaseDatabase.getInstance().reference.child("Tokens")
        val token = Token(refreshToken)
        ref.child(firebaseUser.uid).setValue(token)
    }

    private fun retrieveChatList() {
        users = ArrayList()
        val ref = FirebaseDatabase.getInstance().reference.child("users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.d(TAG, p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                (users as ArrayList).clear()

                for (snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(User::class.java)

                    for (chat in userChatList) {
                        if (user?.uid.equals(chat.id)) {
                            (users as ArrayList).add(user!!)
                        }
                    }
                }

                userAdapter = UserAdapter(true)
                userAdapter.setData(users as ArrayList<User>)
                rv_chatlist?.adapter = userAdapter
            }

        })
    }

}
