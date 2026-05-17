package com.oracle.mrt3.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oracle.mrt3.data.model.EMERGENCY_TYPES
import com.oracle.mrt3.data.model.EmergencyReport
import com.oracle.mrt3.data.model.PersonalContact
import com.oracle.mrt3.ui.components.GenericPickerSheet
import com.oracle.mrt3.ui.components.StationPickerSheet
import com.oracle.mrt3.ui.theme.PrimaryGreen
import com.oracle.mrt3.ui.theme.TextSecondary
import com.oracle.mrt3.viewmodel.EmergencyViewModel
import com.oracle.mrt3.viewmodel.ProfileViewModel
import com.oracle.mrt3.viewmodel.SubmitState

@Composable
fun EmergencyScreen(
    isSignedIn: Boolean,
    emergencyViewModel: EmergencyViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val context         = LocalContext.current
    val reports         by emergencyViewModel.reports.collectAsStateWithLifecycle()
    val submitState     by emergencyViewModel.submitState.collectAsStateWithLifecycle()
    val selectedType    by emergencyViewModel.selectedType.collectAsStateWithLifecycle()
    val selectedStation by emergencyViewModel.selectedStation.collectAsStateWithLifecycle()
    val description     by emergencyViewModel.description.collectAsStateWithLifecycle()
    val contacts        by profileViewModel.contacts.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showReportForm    by remember { mutableStateOf(false) }
    var showTypePicker    by remember { mutableStateOf(false) }
    var showStationPicker by remember { mutableStateOf(false) }
    var newContactName    by remember { mutableStateOf("") }
    var newContactPhone   by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> emergencyViewModel.hasPhoto.value = uri != null }

    LaunchedEffect(submitState) {
        when (val s = submitState) {
            is SubmitState.Success -> {
                snackbarHostState.showSnackbar("Report submitted!")
                emergencyViewModel.resetSubmitState()
                showReportForm = false
            }
            is SubmitState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                emergencyViewModel.resetSubmitState()
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Emergency", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFFEF4444))
            Spacer(Modifier.height(16.dp))

            // ── A. Live Incident Feed ─────────────────────────────────────────
            Text("Live Incidents", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            if (reports.isEmpty()) {
                Text("No active incidents reported.", color = TextSecondary, fontSize = 14.sp)
            } else {
                reports.forEach { report ->
                    IncidentCard(report)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── B. Report Emergency ───────────────────────────────────────────
            Button(
                onClick = { showReportForm = !showReportForm },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (showReportForm) "CANCEL REPORT" else "REPORT EMERGENCY",
                    fontWeight = FontWeight.Bold)
            }

            if (showReportForm) {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Type picker
                        OutlinedTextField(
                            value = selectedType.ifEmpty { "Tap to select type" },
                            onValueChange = {},
                            label = { Text("Emergency Type") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTypePicker = true }
                        )
                        Spacer(Modifier.height(8.dp))

                        // Station picker
                        OutlinedTextField(
                            value = selectedStation.ifEmpty { "Tap to select station" },
                            onValueChange = {},
                            label = { Text("Station") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStationPicker = true }
                        )
                        Spacer(Modifier.height(8.dp))

                        // Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { emergencyViewModel.description.value = it },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4
                        )
                        Spacer(Modifier.height(8.dp))

                        // Photo attachment
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .border(2.dp, PrimaryGreen, RoundedCornerShape(8.dp))
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (emergencyViewModel.hasPhoto.value) "✅  Photo attached"
                                else "📷  Attach Photo (optional)",
                                color = PrimaryGreen
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { emergencyViewModel.submitReport() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            enabled = submitState !is SubmitState.Loading
                        ) {
                            if (submitState is SubmitState.Loading)
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                            else Text("SUBMIT REPORT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── C. Official Hotlines ──────────────────────────────────────────
            Text("Official Hotlines", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            listOf(
                "911"        to "National Emergency",
                "117"        to "Philippine National Police",
                "0284412900" to "MRT-3 Operations",
                "0285275762" to "DOTR Hotline",
                "143"        to "Philippine Red Cross"
            ).forEach { (number, label) ->
                HotlineCard(number, label) {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            // ── D. Personal Contacts (signed-in only) ─────────────────────────
            if (isSignedIn) {
                Spacer(Modifier.height(20.dp))
                Text("Personal Contacts", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))

                // Add contact form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = newContactName,
                            onValueChange = { newContactName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newContactPhone,
                            onValueChange = { newContactPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                profileViewModel.addContact(newContactName, newContactPhone)
                                newContactName = ""
                                newContactPhone = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            enabled = newContactName.isNotBlank() && newContactPhone.isNotBlank()
                        ) {
                            Text("Add Contact")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                contacts.forEach { contact ->
                    ContactCard(
                        contact = contact,
                        onCall = {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                            )
                        },
                        onDelete = { profileViewModel.deleteContact(contact.id) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTypePicker) {
        GenericPickerSheet(
            title    = "Emergency Type",
            options  = EMERGENCY_TYPES,
            selected = selectedType,
            onSelected = { emergencyViewModel.selectedType.value = it },
            onDismiss  = { showTypePicker = false }
        )
    }
    if (showStationPicker) {
        StationPickerSheet(
            selectedStation   = selectedStation,
            onStationSelected = { emergencyViewModel.selectedStation.value = it },
            onDismiss         = { showStationPicker = false }
        )
    }
}

@Composable
private fun IncidentCard(report: EmergencyReport) {
    val relTime = report.createdAt?.let {
        val mins = ((System.currentTimeMillis() - it.toDate().time) / 60_000).toInt()
        when {
            mins < 1  -> "just now"
            mins < 60 -> "${mins}m ago"
            else      -> "${mins / 60}h ago"
        }
    } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFEF4444))
            )
            Column(modifier = Modifier.padding(12.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(report.type, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.weight(1f))
                    Surface(
                        color = if (report.status == "pending") Color(0xFFFFD700)
                                else Color(0xFF00A95C),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            report.status.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = if (report.status == "pending") Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("${report.station} • $relTime", color = TextSecondary, fontSize = 12.sp)
                if (report.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(report.description, fontSize = 13.sp, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun HotlineCard(number: String, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Phone, contentDescription = null, tint = PrimaryGreen)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(number, color = PrimaryGreen, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: PersonalContact,
    onCall: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, fontWeight = FontWeight.SemiBold)
                Text(contact.phone, color = TextSecondary, fontSize = 13.sp)
            }
            IconButton(onClick = onCall) {
                Icon(Icons.Filled.Phone, contentDescription = "Call", tint = PrimaryGreen)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
            }
        }
    }
}
