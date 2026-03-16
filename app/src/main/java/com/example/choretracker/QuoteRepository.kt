package com.example.choretracker

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

class QuoteRepository(
    private val context: Context,
    private val service: QuoteService
) {
    private val prefs =
        context.getSharedPreferences("quote_cache", Context.MODE_PRIVATE)

    private val gson = Gson()
    private val mutex = Mutex()

    companion object {
        private const val RANDOM_QUOTE_KEY = "random_quote"
        private const val RANDOM_QUOTE_FETCH_TIME_KEY = "random_quote_fetch_time"
        private const val RATE_LIMIT_WINDOW_MS = 30_000L
    }

    suspend fun getRandomQuote(): Quote? = mutex.withLock {
        val now = System.currentTimeMillis()

        val cachedQuote = readQuote()
        val lastFetchTime = prefs.getLong(RANDOM_QUOTE_FETCH_TIME_KEY, 0L)

        val canFetch = now - lastFetchTime >= RATE_LIMIT_WINDOW_MS

        if (!canFetch) {
            return cachedQuote
        }

        return try {
            val freshQuote = service.getRandomQuote().firstOrNull()
            if (freshQuote != null) {
                saveQuote(
                    quote = freshQuote,
                    timeValue = now
                )
                freshQuote
            } else {
                cachedQuote
            }
        } catch (e: Exception) {
            cachedQuote
        }
    }

    private fun readQuote(): Quote? {
        val json = prefs.getString(RANDOM_QUOTE_KEY, null) ?: return null
        return try {
            gson.fromJson(json, Quote::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveQuote(
        quote: Quote,
        timeValue: Long
    ) {
        prefs.edit {
            putString(RANDOM_QUOTE_KEY, gson.toJson(quote))
                .putLong(RANDOM_QUOTE_FETCH_TIME_KEY, timeValue)
        }
    }
}