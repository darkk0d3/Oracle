package com.oracle.mrt3.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.oracle.mrt3.R
import com.oracle.mrt3.ui.theme.PrimaryGreen
import com.oracle.mrt3.viewmodel.AuthState
import com.oracle.mrt3.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    activity: Activity,
    onLoginSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    var phoneNumber  by remember { mutableStateOf("") }
    var otpCode      by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(0) }  // seconds remaining
    val snackbarHostState = remember { SnackbarHostState() }

    // Tick down the resend cooldown every second
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1_000L)
            resendCooldown--
        }
    }

    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.Success -> onLoginSuccess()
            is AuthState.OtpSent -> {
                showOtpField   = true
                resendCooldown = 60
            }
            is AuthState.Error -> snackbarHostState.showSnackbar(s.message)
            else -> Unit
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { authViewModel.signInWithGoogle(it) }
        } catch (_: ApiException) { }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryGreen)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter           = painterResource(R.drawable.icon),
                            contentDescription = "Oracle logo",
                            modifier          = Modifier.size(80.dp),
                            contentScale      = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Welcome to Oracle", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("MRT-3 Commuter Guide", fontSize = 14.sp, color = Color(0xFF64748B))
                    Spacer(Modifier.height(24.dp))

                    // Phone input — strip leading 0 hint
                    OutlinedTextField(
                        value         = phoneNumber,
                        onValueChange = {
                            // accept digits only, strip leading 0, max 10 chars
                            val digits = it.filter { c -> c.isDigit() }.trimStart('0').take(10)
                            phoneNumber = digits
                        },
                        label          = { Text("Phone Number") },
                        prefix         = { Text("🇵🇭 +63 ") },
                        placeholder    = { Text("9XXXXXXXXX") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier       = Modifier.fillMaxWidth(),
                        singleLine     = true,
                        enabled        = !showOtpField || authState is AuthState.Idle
                    )
                    Spacer(Modifier.height(12.dp))

                    if (!showOtpField) {
                        // ── Step 1: Send OTP ──────────────────────────────
                        Button(
                            onClick  = { authViewModel.sendOtp(phoneNumber, activity) },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            enabled  = phoneNumber.length == 10 && authState !is AuthState.Loading
                        ) {
                            if (authState is AuthState.Loading)
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                            else Text("Send OTP")
                        }
                    } else {
                        // ── Step 2: Enter OTP ─────────────────────────────
                        OutlinedTextField(
                            value         = otpCode,
                            onValueChange = { if (it.length <= 6) otpCode = it.filter(Char::isDigit) },
                            label          = { Text("6-digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier       = Modifier.fillMaxWidth(),
                            textStyle      = LocalTextStyle.current.copy(
                                textAlign    = TextAlign.Center,
                                letterSpacing = 8.sp
                            ),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick  = { authViewModel.verifyOtp(otpCode) },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            enabled  = otpCode.length == 6 && authState !is AuthState.Loading
                        ) {
                            if (authState is AuthState.Loading)
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                            else Text("Verify Code")
                        }
                        Spacer(Modifier.height(8.dp))

                        // Resend row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Didn't receive it?",
                                fontSize = 13.sp,
                                color    = Color(0xFF64748B)
                            )
                            if (resendCooldown > 0) {
                                Text(
                                    "  Resend in ${resendCooldown}s",
                                    fontSize = 13.sp,
                                    color    = Color(0xFF64748B)
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        otpCode = ""
                                        authViewModel.resendOtp(phoneNumber, activity)
                                    },
                                    enabled = authState !is AuthState.Loading
                                ) {
                                    Text(
                                        "Resend OTP",
                                        fontWeight = FontWeight.SemiBold,
                                        color      = PrimaryGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(activity.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            googleLauncher.launch(GoogleSignIn.getClient(activity, gso).signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border   = BorderStroke(1.dp, Color(0xFFDDDDDD))
                    ) {
                        Text("G   Continue with Google", color = Color(0xFF444444))
                    }
                }
            }
        }
    }
}
