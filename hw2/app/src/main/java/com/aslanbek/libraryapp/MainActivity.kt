package com.aslanbek.libraryapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aslanbek.libraryapp.ui.LibraryApp
import com.aslanbek.libraryapp.ui.theme.LibraryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // savedInstanceState != null means the Activity was recreated (e.g. after rotation)
        Log.d("MainActivity", "onCreate, recreated = ${savedInstanceState != null}")
        enableEdgeToEdge()
        setContent {
            LibraryTheme {
                LibraryApp()
            }
        }
    }

    override fun onDestroy() {
        Log.d("MainActivity", "onDestroy, isChangingConfigurations = $isChangingConfigurations")
        super.onDestroy()
    }
}
