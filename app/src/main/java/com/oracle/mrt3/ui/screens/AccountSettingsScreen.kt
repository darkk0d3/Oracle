package com.oracle.mrt3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.oracle.mrt3.data.model.TripHistoryItem
import com.oracle.mrt3.ui.theme.PrimaryGreen
import com.oracle.mrt3.ui.theme.SecondaryGreen
import com.oracle.mrt3.ui.theme.TextSecondary
import com.oracle.mrt3.viewmodel.ProfileUiState
import com.oracle.mrt3.viewmodel.ProfileViewModel
import com.oracle.mrt3.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    tripViewModel: TripViewModel = hiltViewModel()
) {
    val profile         by profileViewModel.profile.collectAsStateWithLifecycle()
    val tripHistory     by tripViewModel.tripHistory.collectAsStateWithLifecycle()
    val uiState         by profileViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var displayNameField by remember(profile?.displayName) {
        mutableStateOf(profile?.displayName ?: "")
    }
    val isDirty = displayNameField != (profile?.displayName ?: "")
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is ProfileUiState.Success -> {
                snackbarHostState.showSnackbar(s.message)
                profileViewModel.resetUiState()
            }
            is ProfileUiState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                profileViewModel.resetUiState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Account Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryGreen)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(padding)
                .padding(16.dp)
        ) {
            // ── Profile section ───────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))

                        Text("Display Name", color = TextSecondary, fontSize = 12.sp)
                        OutlinedTextField(
                            value = displayNameField,
                            onValueChange = { displayNameField = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Enter your name") }
                        )
                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Email: ${firebaseUser?.email ?: firebaseUser?.phoneNumber ?: "N/A"}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        if (isDirty) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { displayNameField = profile?.displayName ?: "" },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Cancel") }
                                Button(
                                    onClick = { profileViewModel.updateDisplayName(displayNameField) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) { Text("Save") }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Trip History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
            }

            // ── Trip history list ─────────────────────────────────────────────
            if (tripHistory.isEmpty()) {
                item {
                    Text("No trips recorded yet.", color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                items(tripHistory) { trip ->
                    TripHistoryCard(trip)
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun TripHistoryCard(trip: TripHistoryItem) {
    val dateStr = trip.createdAt?.toDate()?.let {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(it)
    } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(SecondaryGreen, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "${trip.origin}  →  ${trip.destination}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "₱${"%.2f".format(trip.fare)}",
                        color = SecondaryGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dateStr, color = TextSecondary, fontSize = 12.sp)
                    if (trip.isDiscounted) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFFFD700),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "DISCOUNTED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
