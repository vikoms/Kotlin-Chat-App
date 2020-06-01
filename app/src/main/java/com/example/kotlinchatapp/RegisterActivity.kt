package com.example.kotlinchatapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.kotlinchatapp.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.lang.Exception
import java.util.*

class RegisterActivity : AppCompatActivity() {

    companion object {
        internal val TAG = RegisterActivity::class.java.simpleName

        //image pick code
        private const val IMAGE_PICK_CODE = 1000

        //Permission code
        private const val PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        tv_already_account.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
        }
        btn_register.setOnClickListener {
            performRegister()
        }


        btn_select_photo.setOnClickListener {
            Log.d(TAG, "Try to show photo selector")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    requestPermissions(permissions, PERMISSION_CODE)
                } else {
//                    Permission already granted
                    pickImageFromGallery()
                }
            } else {
//                System OS is < Marsmellow
                pickImageFromGallery()
            }
        }

    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    pickImageFromGallery()
                } else {
                    Toast.makeText(this@RegisterActivity, "Permission denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun performRegister() {
        val password = edt_password.text.toString()
        val email = edt_email.text.toString()

        if (password.isEmpty() || email.isEmpty()) {
            Toast.makeText(
                this@RegisterActivity,
                "Please enter email or password",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this@RegisterActivity, "Account registered", Toast.LENGTH_SHORT)
                        .show()

                    uploadImageToFirebaseStorage()
                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Register Failed ${task.exception}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnCompleteListener
                }
            }.addOnFailureListener {
                Log.d(TAG, "${it.message}")
                Toast.makeText(this@RegisterActivity, "Register Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) return
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!).addOnSuccessListener {
            Log.d(TAG,"Successfully uploaded image : ${it.metadata?.path}")

            ref.downloadUrl.addOnSuccessListener {
                it.toString()
                Log.d(TAG,"File location:  $it")

                saveUserToDatabase(it.toString())
            }

//
        }
    }

    private fun saveUserToDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User(
            uid,
            edt_username.text.toString(),
            profileImageUrl,
            "online"
        )
        ref.setValue(user).addOnSuccessListener {
            Log.d(TAG , "Finally we save the user to firebase database")
            startActivity(Intent(this@RegisterActivity,MainActivity::class.java))
            finish()
        }
    }

    var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(TAG, "photo was selected")

            selectedPhotoUri = data.data
            try {
                selectedPhotoUri?.let {
                    if (Build.VERSION.SDK_INT < 28) {
                        val bitmap =
                            MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
                        val bitmapDrawable = BitmapDrawable(resources, bitmap)
                        selected_photo_register.setImageBitmap(bitmap)
                        btn_select_photo.alpha = 0f
                    } else {
                        val source = ImageDecoder.createSource(contentResolver, it)
                        val bitmap = ImageDecoder.decodeBitmap(source)
                        val bitmapDrawable = BitmapDrawable(resources, bitmap)
                        selected_photo_register.setImageBitmap(bitmap)
                        btn_select_photo.alpha = 0f
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }


    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(Intent(this@RegisterActivity,MainActivity::class.java))
            finish()
        }
    }
}


