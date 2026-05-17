package com.oracle.mrt3.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.oracle.mrt3.data.model.MRT3_STATIONS
import com.oracle.mrt3.data.model.STATION_ABBREV
import com.oracle.mrt3.ui.theme.GoldAccent
import com.oracle.mrt3.ui.theme.PrimaryGreen
import com.oracle.mrt3.ui.theme.SecondaryGreen
import com.oracle.mrt3.ui.theme.TextSecondary
import com.oracle.mrt3.viewmodel.AuthViewModel
import com.oracle.mrt3.viewmodel.LiveTrainViewModel
import com.oracle.mrt3.viewmodel.ProfileUiState
import com.oracle.mrt3.viewmodel.ProfileViewModel
import com.oracle.mrt3.viewmodel.TripViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    liveTrainViewModel: LiveTrainViewModel,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    tripViewModel: TripViewModel = hiltViewModel()
) {
    val profile         by profileViewModel.profile.collectAsStateWithLifecycle()
    val tripState       by tripViewModel.tripState.collectAsStateWithLifecycle()
    val trainState      by liveTrainViewModel.trainState.collectAsStateWithLifecycle()
    val rating          by profileViewModel.rating.collectAsStateWithLifecycle()
    val feedbackComment by profileViewModel.feedbackComment.collectAsStateWithLifecycle()
    val uiState         by profileViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var elapsedMinutes    by remember { mutableStateOf(0) }

    // Start GPS tracking from Profile too, in case MapScreen was never opened
    LaunchedEffect(tripState.startedAt, tripState.destination) {
        if (tripState.startedAt != null && tripState.destination.isNotEmpty()) {
            liveTrainViewModel.startTracking(tripState.destination)
        }
    }

    // Live elapsed timer
    LaunchedEffect(tripState.startedAt) {
        while (tripState.startedAt != null) {
            elapsedMinutes = ((System.currentTimeMillis() - (tripState.startedAt ?: 0)) / 60_000).toInt()
            delay(30_000L)
        }
    }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is ProfileUiState.Success -> { snackbarHostState.showSnackbar(s.message); profileViewModel.resetUiState() }
            is ProfileUiState.Error   -> { snackbarHostState.showSnackbar(s.message); profileViewModel.resetUiState() }
            else -> Unit
        }
    }

    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val oracleId = firebaseUser?.metadata?.creationTimestamp?.let {
        "#" + SimpleDateFormat("yyyy-MM-ddHHmm", Locale.getDefault()).format(Date(it))
    } ?: "#----"
    val displayName = profile?.displayName ?: firebaseUser?.displayName ?: "Commuter"

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Dark-green header band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(PrimaryGreen)
            )

            // White profile card overlapping header
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-50).dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Person, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(40.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CameraAlt, contentDescription = null,
                                tint = PrimaryGreen, modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(displayName, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(oracleId, color = TextSecondary, fontSize = 12.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text("MRT-3 Commuter", color = TextSecondary, fontSize = 12.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }

            Column(
                modifier = Modifier
                    .offset(y = (-30).dp)
                    .padding(horizontal = 16.dp)
            ) {

                // ── Live Trip Card ──────────────────────────────────────────
                if (tripState.startedAt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFF16211C))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = SecondaryGreen, shape = RoundedCornerShape(20.dp)) {
                                    Text(
                                        "● LIVE",
                                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        color      = Color.White,
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("Active Trip", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "${tripState.origin}  →  ${tripState.destination}",
                                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$elapsedMinutes min elapsed",
                                color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress     = { 0.65f },
                                modifier     = Modifier.fillMaxWidth(),
                                color        = SecondaryGreen,
                                trackColor   = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(16.dp))

                            // ── Live Train Tracker Strip ────────────────────
                            LiveTrainTrackerStrip(trainState = trainState)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Rate Experience ─────────────────────────────────────────
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Rate Your Experience", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            (1..5).forEach { star ->
                                IconButton(onClick = { profileViewModel.setRating(star) }) {
                                    Icon(
                                        imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = "$star stars",
                                        tint     = if (star <= rating) GoldAccent else Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value          = feedbackComment,
                            onValueChange  = profileViewModel::setFeedbackComment,
                            label          = { Text("Comments (optional)") },
                            modifier       = Modifier.fillMaxWidth(),
                            minLines       = 3
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick  = profileViewModel::submitFeedback,
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            enabled  = rating > 0
                        ) {
                            Text("Submit Feedback")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Account Settings row ────────────────────────────────────
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAccountSettings() }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Account Settings", fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Sign Out ────────────────────────────────────────────────
                Button(
                    onClick  = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title   = { Text("Sign out?") },
            text    = { Text("You will be returned to the login screen.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    authViewModel.signOut()
                    onSignOut()
                }) {
                    Text("Sign Out", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Live Train Tracker Strip ──────────────────────────────────────────────────

@Composable
private fun LiveTrainTrackerStrip(trainState: LiveTrainViewModel.TrainState) {
    when {
        trainState.locating -> TrackerShimmer()
        else -> {
            val pulseTransition = rememberInfiniteTransition(label = "pulse")
            val pulseRadius by pulseTransition.animateFloat(
                initialValue = 6f,
                targetValue  = 11f,
                animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
                label        = "pulseRadius"
            )

            Column {
                // Rail line + station dots
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    val totalWidth = constraints.maxWidth.toFloat()
                    val count      = MRT3_STATIONS.size
                    val spacing    = if (count > 1) totalWidth / (count - 1) else 0f
                    val centerY    = constraints.maxHeight / 2f
                    val activeIdx  = trainState.inferredStationIndex

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Rail track
                        drawLine(
                            color       = Color.White.copy(alpha = 0.25f),
                            start       = Offset(0f, centerY),
                            end         = Offset(size.width, centerY),
                            strokeWidth = 3f
                        )
                        // Station dots
                        MRT3_STATIONS.forEachIndexed { i, _ ->
                            val x = i * spacing
                            if (i == activeIdx) {
                                // Pulse ring
                                drawCircle(
                                    color  = Color(0xFFFF9900).copy(alpha = 0.35f),
                                    radius = pulseRadius,
                                    center = Offset(x, centerY)
                                )
                                // Filled dot
                                drawCircle(
                                    color  = Color(0xFFFF9900),
                                    radius = 6f,
                                    center = Offset(x, centerY)
                                )
                            } else {
                                drawCircle(
                                    color  = Color.White.copy(alpha = 0.5f),
                                    radius = 4f,
                                    center = Offset(x, centerY)
                                )
                            }
                        }
                    }
                }

                // Abbreviated station labels
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MRT3_STATIONS.forEachIndexed { i, station ->
                        Text(
                            STATION_ABBREV[station.name] ?: "",
                            fontSize   = 7.sp,
                            color      = if (i == trainState.inferredStationIndex)
                                Color(0xFFFF9900) else Color.White.copy(alpha = 0.5f),
                            maxLines   = 1,
                            overflow   = TextOverflow.Clip,
                            modifier   = Modifier.weight(1f),
                            textAlign  = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Remaining stops + ETA
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (trainState.signalLost) {
                        Text("Signal lost", color = Color.White.copy(0.6f), fontSize = 12.sp)
                    } else if (trainState.inferredStationIndex >= 0) {
                        val stops = trainState.remainingStops
                        Text(
                            if (stops > 0) "$stops stop${if (stops > 1) "s" else ""} remaining"
                            else "Arriving",
                            color    = Color.White,
                            fontSize = 12.sp
                        )
                        if (trainState.etaMinutes >= 0) {
                            Text(
                                " · ~${trainState.etaMinutes} min",
                                color      = SecondaryGreen,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text("Position unavailable", color = Color.White.copy(0.5f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerShimmer() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue  = -600f,
        targetValue   = 600f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label         = "shimmerX"
    )
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(0.04f),
                            Color.White.copy(0.14f),
                            Color.White.copy(0.04f)
                        ),
                        startX = translateX,
                        endX   = translateX + 400f
                    )
                )
        )
        Spacer(Modifier.height(8.dp))
        Text("Locating…", color = Color.White.copy(0.55f), fontSize = 12.sp)
    }
}
