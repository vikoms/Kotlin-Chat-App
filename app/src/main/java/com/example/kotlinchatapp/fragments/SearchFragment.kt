package com.example.kotlinchatapp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.kotlinchatapp.R
import com.example.kotlinchatapp.entity.User
import com.example.kotlinchatapp.adapter.UserAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_search.*
import java.util.*
import kotlin.collections.ArrayList

class SearchFragment : Fragment() {

    private lateinit var users: List<User>
    private lateinit var userAdapter: UserAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (activity != null) {
            users = ArrayList()

            rv_list_search.setHasFixedSize(true)
            rv_list_search.layoutManager = LinearLayoutManager(activity)
            retrieveAllUser()

            edt_search_user.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {

                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    searchUsers(
                        edt_search_user.text.toString().toLowerCase(Locale.getDefault()).trim()
                    )

//                    Log.d("search_user",edt_search_user.text.toString().toLowerCase(Locale.getDefault()).trim())
                }

            })
        }
    }

    private fun retrieveAllUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val refUsers = FirebaseDatabase.getInstance().getReference("users")
        refUsers.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(activity, "Retrieve data error ${p0.message}", Toast.LENGTH_SHORT)
                    .show()
                Log.d("searchFragment", "Retrieve data error ${p0.message}")
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                (users as ArrayList<User>).clear()
                val searchUser = edt_search_user?.text.toString().trim()
                if (TextUtils.isEmpty(searchUser)) {
                    for (snapshot in dataSnapshot.children) {
                        val user = snapshot.getValue(User::class.java)

                        if (!(user?.uid).equals(uid)) {
                            if (user != null) {
                                (users as ArrayList<User>).add(user)
                            }
                        }
                    }

                    userAdapter = UserAdapter(false)
                    userAdapter.setData(users as ArrayList<User>)
                    rv_list_search?.adapter = userAdapter

                }
            }

        })
    }

    private fun searchUsers(query: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val queryUser = FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("username")
            .startAt(query)
            .endAt("$query\uf8ff")
        queryUser.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                (users as ArrayList<User>).clear()
                    for (snapshot in dataSnapshot.children) {
                        val user = snapshot.getValue(User::class.java)
                        if (!(user?.uid).equals(uid)) {
                            if (user != null) {
                                (users as ArrayList<User>).add(user)
                            }
                    }
                    userAdapter = UserAdapter(false)
                        userAdapter.setData(users as ArrayList<User>)
                    rv_list_search?.adapter = userAdapter
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Log.d("searchFragment", "Retrieve data error ${p0.message}")
            }


        })
    }
}
