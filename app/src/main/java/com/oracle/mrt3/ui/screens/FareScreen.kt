package com.oracle.mrt3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oracle.mrt3.data.model.PASSENGER_TYPES
import com.oracle.mrt3.ui.components.GenericPickerSheet
import com.oracle.mrt3.ui.components.StationPickerSheet
import com.oracle.mrt3.ui.theme.GoldAccent
import com.oracle.mrt3.ui.theme.PrimaryGreen
import com.oracle.mrt3.ui.theme.SecondaryGreen
import com.oracle.mrt3.ui.theme.TextSecondary
import com.oracle.mrt3.viewmodel.SaveState
import com.oracle.mrt3.viewmodel.TripViewModel

@Composable
fun FareScreen(
    onNavigateToMap: () -> Unit = {},
    viewModel: TripViewModel = hiltViewModel()
) {
    val tripState by viewModel.tripState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showOriginPicker by remember { mutableStateOf(false) }
    var showDestPicker   by remember { mutableStateOf(false) }
    var showTypePicker   by remember { mutableStateOf(false) }
    var passengerType    by remember { mutableStateOf("2026 Special Discount") }

    LaunchedEffect(Unit) { viewModel.setPassengerType(true) }

    LaunchedEffect(saveState) {
        when (val s = saveState) {
            is SaveState.Success -> {
                viewModel.resetSaveState()
                onNavigateToMap()
            }
            is SaveState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetSaveState()
            }
            else -> Unit
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(padding)
        ) {
            // Dark-green rounded-bottom header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PrimaryGreen,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Fare Calculator",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Input card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FareRow("FROM", tripState.origin.ifEmpty { "Select station" }) {
                            showOriginPicker = true
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        FareRow("TO", tripState.destination.ifEmpty { "Select station" }) {
                            showDestPicker = true
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        FareRow("PASSENGER TYPE", passengerType) {
                            showTypePicker = true
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Train banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF16211C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚇  MRT-3", color = Color.White, fontSize = 28.sp)
                }

                // Result card — only shown when both stations are selected
                if (tripState.origin.isNotEmpty() && tripState.destination.isNotEmpty()
                    && tripState.origin != tripState.destination
                ) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(5.dp, SecondaryGreen, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "₱${"%.2f".format(tripState.fare)}",
                                color = PrimaryGreen,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            if (tripState.isDiscounted) {
                                Surface(
                                    color = GoldAccent,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        "2026 SPECIAL: 50% DISCOUNT",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Surface(
                                    color = SecondaryGreen,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        "GENERAL SUBSIDY APPLIED",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.saveTrip() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                enabled = saveState !is SaveState.Loading
                            ) {
                                if (saveState is SaveState.Loading)
                                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                                else Text("SAVE & VIEW ON MAP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOriginPicker) {
        StationPickerSheet(
            selectedStation = tripState.origin,
            onStationSelected = viewModel::setOrigin,
            onDismiss = { showOriginPicker = false }
        )
    }
    if (showDestPicker) {
        StationPickerSheet(
            selectedStation = tripState.destination,
            onStationSelected = viewModel::setDestination,
            onDismiss = { showDestPicker = false }
        )
    }
    if (showTypePicker) {
        GenericPickerSheet(
            title    = "Passenger Type",
            options  = PASSENGER_TYPES.map { it.first },
            selected = passengerType,
            onSelected = { type ->
                passengerType = type
                val isDiscounted = PASSENGER_TYPES.find { it.first == type }?.second ?: false
                viewModel.setPassengerType(isDiscounted)
            },
            onDismiss = { showTypePicker = false }
        )
    }
}

@Composable
private fun FareRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            color = if (value.startsWith("Select")) TextSecondary else Color.Black,
            fontWeight = if (value.startsWith("Select")) FontWeight.Normal else FontWeight.Medium
        )
    }
}
