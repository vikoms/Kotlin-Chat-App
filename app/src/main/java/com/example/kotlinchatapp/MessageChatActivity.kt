package com.example.kotlinchatapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.kotlinchatapp.adapter.ChatAdapter
import com.example.kotlinchatapp.entity.Chat
import com.example.kotlinchatapp.entity.User
import com.example.kotlinchatapp.notifications.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_message_chat.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessageChatActivity : AppCompatActivity() {

    companion object {
        const val VISIT_ID = "VISIT_ID"
        internal val TAG = MessageChatActivity::class.java.simpleName


        //image pick code
        private const val IMAGE_PICK_CODE = 1000
    }

    private var userIdVisit: String? = null
    private var mUser: FirebaseUser? = null
    private lateinit var reference: DatabaseReference
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatList: List<Chat>

    private var notify = false
    private var apiService: ApiService? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_chat)
        userIdVisit = intent.getStringExtra(VISIT_ID)
        mUser = FirebaseAuth.getInstance().currentUser

        setSupportActionBar(toolbar_chat)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar_chat.setNavigationOnClickListener {
            val intent = Intent(this@MessageChatActivity,MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        apiService =Client.getClient("https://fcm.googleapis.com/")?.create(ApiService::class.java)

        with(rv_chat) {
            setHasFixedSize(true)
            val linearLayoutManager = LinearLayoutManager(this@MessageChatActivity)
            linearLayoutManager.stackFromEnd = true
            layoutManager = linearLayoutManager
        }
        reference = FirebaseDatabase.getInstance().reference.child("users").child(userIdVisit!!)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.d(TAG, p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                tv_username_chat.text = user?.username
                Glide.with(applicationContext).load(user?.profileImageUrl)
                    .into(img_profile_chat)

                retrieveMessage(mUser?.uid, userIdVisit!!, user?.profileImageUrl)
            }

        })


        img_send_message.setOnClickListener {
            val message = edt_text_message.text.toString()
            notify = true
            if (TextUtils.isEmpty(message)) {
                Toast.makeText(
                    this@MessageChatActivity,
                    "Please write message, first ...",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                sendMessageToUser(mUser?.uid, userIdVisit, message)
            }
            edt_text_message.setText("")
        }

        img_attach_image_file.setOnClickListener {
            notify = true
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Pick Image"), IMAGE_PICK_CODE)
        }
        seenMessage(userIdVisit!!)
    }

    private fun retrieveMessage(senderId: String?, receiverId: String, receiverImageUrl: String?) {
        chatList = ArrayList()
        val reference = FirebaseDatabase.getInstance().getReference("Chats")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.d(TAG, p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                (chatList as ArrayList<Chat>).clear()
                for (snapshot in dataSnapshot.children) {
                    val chat = snapshot.getValue(Chat::class.java)
                    if (chat?.receiver.equals(senderId) && chat?.sender.equals(receiverId)
                        || chat?.receiver.equals(receiverId) && chat?.sender.equals(senderId)
                    ) {
                        (chatList as ArrayList<Chat>).add(chat!!)
                    }
                    chatAdapter = ChatAdapter(receiverImageUrl!!)
                    chatAdapter.setData(chatList as ArrayList<Chat>)
                    rv_chat.adapter = chatAdapter
                }
            }

        })
    }

    private fun sendMessageToUser(
        senderId: String?,
        receiveId: String?,
        message: String
    ) {
        val reference = FirebaseDatabase.getInstance().reference
        val messageKey = reference.push().key

        val messageHashMap = HashMap<String, Any?>()
        messageHashMap["sender"] = senderId
        messageHashMap["message"] = message
        messageHashMap["receiver"] = receiveId
        messageHashMap["isseen"] = false
        messageHashMap["url"] = ""
        messageHashMap["messageId"] = messageKey
        reference.child("Chats").child(messageKey!!).setValue(messageHashMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val chatListReference =
                        FirebaseDatabase.getInstance().reference.child("ChatList")
                            .child(mUser!!.uid)
                            .child(userIdVisit!!)

                    chatListReference.addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                            Log.d(TAG, p0.message)
                        }

                        override fun onDataChange(p0: DataSnapshot) {
                            if (!p0.exists()) {
                                chatListReference.child("id").setValue(userIdVisit)
                            }

                            val chatListReceiverReference =
                                FirebaseDatabase.getInstance().reference.child("ChatList")
                                    .child(userIdVisit!!)
                                    .child(mUser!!.uid)
                            chatListReceiverReference.child("id").setValue(mUser?.uid)
                        }

                    })

                }
            }


//                    Implement the push notification
        val userReference =
            FirebaseDatabase.getInstance().reference.child("users").child(mUser!!.uid)

        userReference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.d(TAG, p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                if (notify) {
                    sendNotification(receiveId, user?.username, message)
                }
                notify = false
            }

        })

    }

    private fun sendNotification(receiveId: String?, username: String?, message: String) {
        val ref = FirebaseDatabase.getInstance().getReference("Tokens")
        val query = ref.orderByKey().equalTo(receiveId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.d(TAG, p0.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val token = snapshot.getValue(Token::class.java)
                    val data = Data(
                        mUser?.uid,
                        R.mipmap.ic_launcher,
                        "$username : $message",
                        "New Message",
                        userIdVisit
                    )

                    val sender = Sender(data, token?.token.toString())
                    apiService?.sendNotification(sender)
                        ?.enqueue(object : Callback<MyResponse> {
                            override fun onFailure(call: Call<MyResponse>, t: Throwable) {
                                Log.d(TAG,t.message)
                            }

                            override fun onResponse(
                                call: Call<MyResponse>,
                                response: Response<MyResponse>
                            ) {
                                if (response.code() == 200) {
                                    if (response.body()?.success != 1) {
                                        Toast.makeText(this@MessageChatActivity,"Failed, nothing happen",Toast.LENGTH_SHORT).show()

                                    }
                                }
                            }

                        })
                }
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val loadingBar = ProgressDialog(this)
            loadingBar.setMessage("Please wait, image is sending ...")
            loadingBar.show()

            val fileUri = data.data
            val storageReference = FirebaseStorage.getInstance().reference.child("Chat Images")
            val ref = FirebaseDatabase.getInstance().reference
            val messageId = ref.push().key
            val filePath = storageReference.child("$messageId.jpg")
            filePath.putFile(fileUri!!).addOnSuccessListener { it ->
                Log.d(RegisterActivity.TAG, "Successfully uploaded image : ${it.metadata?.path}")
                filePath.downloadUrl.addOnSuccessListener {
                    val url = it.toString()
                    val messageHashMap = HashMap<String, Any?>()
                    messageHashMap["sender"] = mUser?.uid
                    messageHashMap["message"] = "sent you an image."
                    messageHashMap["receiver"] = userIdVisit
                    messageHashMap["isseen"] = false
                    messageHashMap["url"] = url
                    messageHashMap["messageId"] = messageId

                    ref.child("Chats").child(messageId!!).setValue(messageHashMap)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {

                                loadingBar.dismiss()

//                                Implement the push notification
                                val reference =
                                    FirebaseDatabase.getInstance().reference.child("users")
                                        .child(mUser!!.uid)

                                reference.addValueEventListener(object : ValueEventListener {
                                    override fun onCancelled(p0: DatabaseError) {
                                        Log.d(TAG, p0.message)
                                    }

                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        val user = dataSnapshot.getValue(User::class.java)
                                        if (notify) {
                                            sendNotification(
                                                userIdVisit,
                                                user?.username,
                                                "sent you an image."
                                            )
                                        }
                                        notify = false
                                    }

                                })
                            }
                        }
                }
            }

        }
    }

    private var seenListener: ValueEventListener? = null
    private fun seenMessage(userId: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("Chats")
        seenListener = reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.d(TAG, p0.message)
            }

            override fun onDataChange(datasnapshot: DataSnapshot) {
                for (snapshot in datasnapshot.children) {
                    val chat = snapshot.getValue(Chat::class.java)
                    if (chat?.receiver.equals(mUser?.uid) && chat?.sender.equals(userId)) {
                        val hashMap = HashMap<String, Any>()
                        hashMap["isseen"] = true
                        snapshot.ref.updateChildren(hashMap)
                    }
                }

            }

        })
    }

    override fun onPause() {
        super.onPause()
        reference.removeEventListener(seenListener!!)
    }


}
