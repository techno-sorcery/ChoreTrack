package com.example.choretracker

import android.app.Application
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainApplication : Application() {

    lateinit var quoteRepository: QuoteRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val quoteService: QuoteService by lazy {

            val retrofit = Retrofit.Builder()
                .baseUrl("https://zenquotes.io/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(QuoteService::class.java)
        }

        quoteRepository = QuoteRepository(
            applicationContext,
            quoteService
        )
    }
}