package com.oracle.mrt3.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    object OtpSent : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    init {
        auth.addAuthStateListener { _currentUser.value = it.currentUser }
    }

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading
        val cleanNumber = "+63${phoneNumber.trimStart('0')}"
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(cleanNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(buildCallbacks())
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun resendOtp(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading
        val cleanNumber = "+63${phoneNumber.trimStart('0')}"
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(cleanNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(buildCallbacks())
        resendToken?.let { builder.setForceResendingToken(it) }
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun verifyOtp(code: String) {
        val id = verificationId ?: run {
            _authState.value = AuthState.Error("Session expired. Tap Resend OTP.")
            return
        }
        signInWithPhoneCredential(PhoneAuthProvider.getCredential(id, code))
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google sign-in failed")
            }
        }
    }

    private fun buildCallbacks() = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneCredential(credential)
        }
        override fun onVerificationFailed(e: FirebaseException) {
            _authState.value = AuthState.Error(e.message ?: "OTP failed")
        }
        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
            verificationId = id
            resendToken    = token
            _authState.value = AuthState.OtpSent
        }
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                auth.signInWithCredential(credential).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Verification failed")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
