package com.example.kotlinchatapp

import com.example.kotlinchatapp.notifications.MyResponse
import com.example.kotlinchatapp.notifications.Sender
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {


    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAAxZekNdI:APA91bG51S-kkNaBHUS752r1wGsCYJFbIUS12zN5dJwDXxaiji_HEOWFpJC-dy1qrMNO0raxCL3fKlhoEN2cxSL65CLSv7ATLtD_9tbCgbl7ud7oim0sfIN0i88oeyhg73VmWmG-tWMH"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: Sender): Call<MyResponse>

}