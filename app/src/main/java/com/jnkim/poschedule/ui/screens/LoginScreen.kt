package com.jnkim.poschedule.ui.screens

import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jnkim.poschedule.ui.components.GlassBackground
import com.jnkim.poschedule.ui.components.GlassCard
import com.jnkim.poschedule.ui.theme.ModeNormal
import com.jnkim.poschedule.ui.viewmodel.AuthUiState
import com.jnkim.poschedule.ui.viewmodel.AuthViewModel

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var showWebView by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf("https://genai.postech.ac.kr/auth/login") }
    var isPageLoading by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            Log.d(TAG, "Auth success, navigating to main screen")
            onLoginSuccess()
        }
    }

    GlassBackground(accentColor = ModeNormal) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!showWebView) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Poschedule",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "A gentle OS for graduate life",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { 
                                    Log.d(TAG, "Login button clicked, showing WebView")
                                    showWebView = true 
                                },
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ModeNormal.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                            ) {
                                Text("Login with POSTECH SSO")
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "SSO Login",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = currentUrl,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(onClick = { 
                                    Log.d(TAG, "Cancel clicked")
                                    showWebView = false 
                                }) {
                                    Text("Cancel")
                                }
                            }
                            if (isPageLoading) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ModeNormal,
                                    trackColor = Color.Transparent
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        SsoWebView(
                            url = "https://genai.postech.ac.kr/auth/login",
                            onUrlChanged = { currentUrl = it },
                            onLoadingChanged = { isPageLoading = it },
                            onTokenAcquired = { token ->
                                Log.d(TAG, "Token acquired from WebView")
                                viewModel.onTokenReceived(token)
                            }
                        )
                    }
                }
            }

            if (uiState is AuthUiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ModeNormal)
                }
            }
        }
    }
}

@Composable
fun SsoWebView(
    url: String,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onTokenAcquired: (String) -> Unit
) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d(TAG, "Page started: $url")
                    onLoadingChanged(true)
                    url?.let { onUrlChanged(it) }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page finished: $url")
                    onLoadingChanged(false)
                    url?.let {
                        onUrlChanged(it)
                        if (it.contains("#access_token=")) {
                            Log.d(TAG, "Detected access_token in URL fragment")
                            val fragment = it.substringAfter("#")
                            val params = fragment.split("&").associate { param ->
                                val parts = param.split("=")
                                parts[0] to parts.getOrElse(1) { "" }
                            }
                            val token = params["access_token"]
                            if (token != null) {
                                Log.d(TAG, "Successfully extracted access_token")
                                onTokenAcquired(token)
                            } else {
                                Log.e(TAG, "Failed to extract access_token from fragment")
                            }
                        }
                    }
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView error: $errorCode, $description, $failingUrl")
                }
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            loadUrl(url)
        }
    })
}
