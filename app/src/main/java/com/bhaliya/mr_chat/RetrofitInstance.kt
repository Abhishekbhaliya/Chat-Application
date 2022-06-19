package com.bhaliya.mr_chat

import com.bhaliya.mr_chat.Constants.Constants
import com.bhaliya.mr_chat.`interface`.NotificationApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class RetrofitInstance {

    companion object{

        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        val api by lazy {
            retrofit.create(NotificationApi::class.java)
        }

    }
}