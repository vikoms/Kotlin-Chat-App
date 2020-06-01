package com.example.kotlinchatapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_view_full_image.*

class ViewFullImageActivity : AppCompatActivity() {

    companion object {
        const val IMAGE_URL = "IMAGE_URL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_full_image)

        val imageUrl = intent.getStringExtra(IMAGE_URL)
        Glide.with(this).load(imageUrl).into(img_viewer)
    }
}
