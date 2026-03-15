package com.example.choretracker

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        private const val TODAY_QUOTE_KEY = "today_quote"
        private const val TODAY_QUOTE_DATE_KEY = "today_quote_date"

        private const val RATE_LIMIT_WINDOW_MS = 30_000L
    }

    suspend fun getRandomQuote(): Quote? = mutex.withLock {
        val now = System.currentTimeMillis()

        val cachedQuote = readQuote(RANDOM_QUOTE_KEY)
        val lastFetchTime = prefs.getLong(RANDOM_QUOTE_FETCH_TIME_KEY, 0L)

        val canFetch = now - lastFetchTime >= RATE_LIMIT_WINDOW_MS

        if (!canFetch) {
            return cachedQuote
        }

        return try {
            val freshQuote = service.getRandomQuote().firstOrNull()
            if (freshQuote != null) {
                saveQuote(
                    quoteKey = RANDOM_QUOTE_KEY,
                    quote = freshQuote,
                    timeKey = RANDOM_QUOTE_FETCH_TIME_KEY,
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

    suspend fun getTodayQuote(): Quote? = mutex.withLock {
        val today = todayString()

        val cachedDate = prefs.getString(TODAY_QUOTE_DATE_KEY, null)
        val cachedQuote = readQuote(TODAY_QUOTE_KEY)

        if (cachedDate == today && cachedQuote != null) {
            return cachedQuote
        }

        return try {
            val freshQuote = service.getTodayQuote().firstOrNull()
            if (freshQuote != null) {
                prefs.edit()
                    .putString(TODAY_QUOTE_KEY, gson.toJson(freshQuote))
                    .putString(TODAY_QUOTE_DATE_KEY, today)
                    .apply()
                freshQuote
            } else {
                cachedQuote
            }
        } catch (e: Exception) {
            cachedQuote
        }
    }

    private fun readQuote(key: String): Quote? {
        val json = prefs.getString(key, null) ?: return null
        return try {
            gson.fromJson(json, Quote::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveQuote(
        quoteKey: String,
        quote: Quote,
        timeKey: String,
        timeValue: Long
    ) {
        prefs.edit()
            .putString(quoteKey, gson.toJson(quote))
            .putLong(timeKey, timeValue)
            .apply()
    }

    private fun todayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}