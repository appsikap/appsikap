package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.AppNavigation
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database, DAOs, repository, and modern ViewModel
        val database = AppDatabase.getDatabase(this)
        val repository = AppRepository(
            adminDao = database.adminDao(),
            siswaDao = database.siswaDao(),
            kategoriDao = database.kategoriAktivitasDao(),
            riwayatDao = database.riwayatPoinDao(),
            pengaturanDao = database.pengaturanSekolahDao(),
            hadiahDao = database.hadiahDao(),
            pengajuanHadiahDao = database.pengajuanHadiahDao()
        )
        val viewModelFactory = MainViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
        
        // Load settings & sessions
        viewModel.loadThemeSettings(this)
        viewModel.initSession(this)
        
        enableEdgeToEdge()
        
        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDark) {
                AppNavigation(viewModel)
            }
        }
    }
}
