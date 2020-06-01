package com.example.kotlinchatapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlinchatapp.MessageChatActivity
import com.example.kotlinchatapp.R
import com.example.kotlinchatapp.ViewFullImageActivity
import com.example.kotlinchatapp.entity.Chat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class ChatAdapter(private val imageUrl: String) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val chatList = ArrayList<Chat>()
    private var firebaseUser: FirebaseUser = FirebaseAuth.getInstance().currentUser!!
    fun setData(chatList: ArrayList<Chat>) {
        this.chatList.clear()
        this.chatList.addAll(chatList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatList[position].sender.equals(firebaseUser.uid)) {
            1
        } else {
            0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        return if (viewType == 1) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_right, parent, false)
            ChatViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_left, parent, false)
            ChatViewHolder(view)
        }
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]

        Glide.with(holder.itemView.context).load(imageUrl).into(holder.profile_image!!)
        if (chat.message.equals("sent you an image.") && !chat.url.equals("")) {
//        Right Message
            if (chat.sender.equals(firebaseUser.uid)) {
                holder.show_text_message?.visibility = View.GONE
                holder.right_image_view?.visibility = View.VISIBLE
                Glide.with(holder.itemView.context).load(chat.url)
                    .into(holder.right_image_view!!)

                holder.right_image_view?.setOnClickListener {
                    val options = arrayOf<CharSequence>(
                        "View Full Image",
                        "Delete Image",
                        "Cancel"
                    )
                    val builder = MaterialAlertDialogBuilder(holder.itemView.context)
                    builder.setTitle("What do you want ? ")
                    builder.setItems(options) { dialog, which ->
                        if (which == 0) {
                            val intent = Intent(
                                holder.itemView.context,
                                ViewFullImageActivity::class.java
                            ).apply {
                                putExtra(ViewFullImageActivity.IMAGE_URL, chat.url)
                            }
                            holder.itemView.context.startActivity(intent)
                        } else if (which == 1) {
                            deleteSentMessage(position, holder)
                        } else {
                            dialog.cancel()
                        }
                    }
                    builder.show()
                }
            }
//        Left Message
            else if (!chat.sender.equals(firebaseUser.uid)) {
                holder.show_text_message?.visibility = View.GONE
                holder.left_image_view?.visibility = View.VISIBLE
                Glide.with(holder.itemView.context).load(chat.url)
                    .into(holder.left_image_view!!)

                holder.left_image_view?.setOnClickListener {
                    val options = arrayOf<CharSequence>(
                        "View Full Image",
                        "Cancel"
                    )
                    val builder = MaterialAlertDialogBuilder(holder.itemView.context)
                    builder.setTitle("What do you want ? ")
                    builder.setItems(options) { dialog, which ->
                        if (which == 0) {
                            val intent = Intent(
                                holder.itemView.context,
                                ViewFullImageActivity::class.java
                            ).apply {
                                putExtra(ViewFullImageActivity.IMAGE_URL, chat.url)
                            }
                            holder.itemView.context.startActivity(intent)
                        } else {
                            dialog.cancel()
                        }
                    }
                    builder.show()
                }
            }
        } else {
            holder.show_text_message?.text = chat.message

            if (firebaseUser.uid == chat.sender) {
                holder.show_text_message?.setOnClickListener {
                    val options = arrayOf<CharSequence>(
                        "Delete Message",
                        "Cancel"
                    )
                    val builder = MaterialAlertDialogBuilder(holder.itemView.context)
                    builder.setTitle("What do you want ? ")
                    builder.setItems(options) { dialog, which ->
                        if(which == 0) {
                            deleteSentMessage(position,holder)
                        } else {
                            dialog.cancel()
                        }
                    }
                    builder.show()
                }
            }
        }

//        Sent and seen message
        if (position == chatList.size - 1) {
            if (chat.isseen) {
                holder.text_seen?.text = "Seen"
                if (chat.message.equals("sent you an image.") && !chat.url.equals("")) {
                    val lp = holder.text_seen?.layoutParams as RelativeLayout.LayoutParams?
                    lp?.setMargins(0, 245, 10, 0)
                    holder.text_seen?.layoutParams = lp
                }
            } else {
                holder.text_seen?.text = "Sent"
                if (chat.message.equals("sent you an image.") && !chat.url.equals("")) {
                    val lp = holder.text_seen?.layoutParams as RelativeLayout.LayoutParams?
                    lp?.setMargins(0, 260, 25, 0)
                    holder.text_seen?.layoutParams = lp
                }
            }
        } else {
            holder.text_seen?.visibility = View.GONE
        }
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var profile_image: CircleImageView? = null
        var show_text_message: TextView? = null
        var left_image_view: ImageView? = null
        var text_seen: TextView? = null
        var right_image_view: ImageView? = null

        init {
            profile_image = itemView.findViewById(R.id.img_profile)
            show_text_message = itemView.findViewById(R.id.tv_text_message)
            left_image_view = itemView.findViewById(R.id.img_left_message)
            text_seen = itemView.findViewById(R.id.tv_seen)
            right_image_view = itemView.findViewById(R.id.img_right_message)
        }
    }

    private fun deleteSentMessage(position: Int, holder: ChatAdapter.ChatViewHolder) {
        val ref = FirebaseDatabase.getInstance().reference.child("Chats")
            .child(chatList[position].messageId!!)
            .removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(holder.itemView.context, "Deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        holder.itemView.context,
                        "Failed, Not Deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

}