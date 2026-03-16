package com.example.choretracker
import retrofit2.http.GET

interface QuoteService {

  @GET("random")
  suspend fun getRandomQuote(): List<Quote>
}