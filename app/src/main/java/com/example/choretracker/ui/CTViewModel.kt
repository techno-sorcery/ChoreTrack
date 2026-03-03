package com.example.choretracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.choretracker.AppDestinations
import com.example.choretracker.Chore
import com.example.choretracker.Person

class CTViewModel : ViewModel() {
    var screen by mutableStateOf(AppDestinations.HOME)
    val choreList = mutableStateListOf<Chore>()
    val personList = mutableStateListOf<Person>()
}