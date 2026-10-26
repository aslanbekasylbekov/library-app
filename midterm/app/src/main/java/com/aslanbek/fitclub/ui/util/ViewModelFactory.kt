package com.aslanbek.fitclub.ui.util

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.aslanbek.fitclub.FitClubApplication
import com.aslanbek.fitclub.di.AppContainer

fun CreationExtras.appContainer(): AppContainer =
    (checkNotNull(this[APPLICATION_KEY]) as FitClubApplication).container
