package com.example.financesharing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.financesharing.presentation.auth.AuthViewModel
import com.example.financesharing.ui.theme.FinanceSharingTheme
import com.example.financesharing.presentation.navigation.GiftShareNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceSharingTheme {
                GiftShareApp()
            }
        }
    }
}

@Composable
fun GiftShareApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    GiftShareNavGraph(
        navController = navController,
        authViewModel = authViewModel,
        modifier = Modifier.fillMaxSize()
    )
}

@Preview(showBackground = true)
@Composable
fun GiftSharePreview() {
    FinanceSharingTheme {
        GiftShareApp()
    }
}