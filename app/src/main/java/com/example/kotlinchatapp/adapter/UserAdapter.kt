package com.example.kotlinchatapp.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlinchatapp.MessageChatActivity
import com.example.kotlinchatapp.R
import com.example.kotlinchatapp.VisitUserActivity
import com.example.kotlinchatapp.entity.Chat
import com.example.kotlinchatapp.entity.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.search_item.view.*

class UserAdapter(private val isChatCheck: Boolean) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val users = ArrayList<User>()
    var lastMessage: String? = null
    fun setData(users: ArrayList<User>) {
        this.users.clear()
        this.users.addAll(users)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_item, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
        if (isChatCheck) {
            retrieveLastMessage(user.uid, holder.itemView.tv_message_last)
        } else {
            holder.itemView.tv_message_last.visibility = View.GONE
        }

        if (isChatCheck) {
            if (user.status == "online") {
                holder.itemView.img_online.visibility = View.VISIBLE
            } else {
                holder.itemView.img_online.visibility = View.GONE
                holder.itemView.img_offline.visibility = View.VISIBLE
            }
        } else {
            holder.itemView.img_online.visibility = View.GONE
            holder.itemView.img_offline.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val options = arrayOf<CharSequence>(
                "Send Message",
                "Visit Profile"
            )
            val builder = MaterialAlertDialogBuilder(holder.itemView.context)
            builder.setTitle("What do you want ? ")
            builder.setItems(options) { dialog, which ->
                if (which == 0) {
                    val intent =
                        Intent(holder.itemView.context, MessageChatActivity::class.java).apply {
                            putExtra(MessageChatActivity.VISIT_ID, users[position].uid)
                        }
                    holder.itemView.context.startActivity(intent)
                } else if (which == 1) {
                    val intent =
                        Intent(holder.itemView.context, VisitUserActivity::class.java).apply {
                            putExtra(VisitUserActivity.USER_ID, users[position].uid)
                        }
                    holder.itemView.context.startActivity(intent)
                }
            }
            builder.show()
        }
    }


    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(data: User) {
            with(itemView) {
                tv_username.text = data.username
                Glide.with(itemView.context).load(data.profileImageUrl)
                    .into(img_profile)
            }
        }
    }


    private fun retrieveLastMessage(chatUserId: String?, tvLastMessage: TextView?) {
        lastMessage = "defaultMessage"

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val referece = FirebaseDatabase.getInstance().reference.child("Chats")

        referece.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.d("MainActivity", p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (datasnapshot in p0.children) {
                    val chat = datasnapshot.getValue(Chat::class.java)

                    if (firebaseUser != null && chat != null) {
                        if (chat.receiver == firebaseUser.uid
                            && chat.sender == chatUserId ||
                            chat.receiver == chatUserId &&
                            chat.sender == firebaseUser.uid
                        ) {
                            lastMessage = chat.message

                        }
                    }
                }


                when(lastMessage) {
                    "defaultMessage" -> tvLastMessage?.text = "No Message"
                    "sent you an image."->tvLastMessage?.text = "Image sent."
                    else -> tvLastMessage?.text = lastMessage
                }

                lastMessage = "defaultMessage"
            }

        })
    }

}