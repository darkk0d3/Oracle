package com.oracle.mrt3.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oracle.mrt3.data.model.MRT3_STATIONS
import com.oracle.mrt3.data.model.STATION_NAMES
import com.oracle.mrt3.data.model.StationStatus
import com.oracle.mrt3.data.model.stationStatusFromString
import com.oracle.mrt3.ui.components.StationPickerSheet
import com.oracle.mrt3.ui.theme.PrimaryGreen
import com.oracle.mrt3.ui.theme.TextSecondary
import com.oracle.mrt3.viewmodel.LiveTrainViewModel
import com.oracle.mrt3.viewmodel.TripViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen(
    liveTrainViewModel: LiveTrainViewModel,
    onExitMap: () -> Unit,
    viewModel: TripViewModel = hiltViewModel()
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tripState  by viewModel.tripState.collectAsStateWithLifecycle()
    val statuses   by viewModel.stationStatuses.collectAsStateWithLifecycle()
    val trainState by liveTrainViewModel.trainState.collectAsStateWithLifecycle()

    var showOriginPicker by remember { mutableStateOf(false) }
    var showDestPicker   by remember { mutableStateOf(false) }
    var mapOrigin        by remember { mutableStateOf(tripState.origin) }
    var mapDest          by remember { mutableStateOf(tripState.destination) }

    // Sync from ViewModel when navigating from FareScreen.
    // Needed because restoreState=true in navigation can replay stale remember values.
    LaunchedEffect(tripState.origin, tripState.destination) {
        if (tripState.origin.isNotEmpty()) mapOrigin = tripState.origin
        if (tripState.destination.isNotEmpty()) mapDest = tripState.destination
    }

    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var backgroundLocationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val fgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> locationGranted = granted }

    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> backgroundLocationGranted = granted }

    // Request foreground location on first entry
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        if (!locationGranted) fgLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Request background location after foreground is granted (Android 10+)
    LaunchedEffect(locationGranted) {
        if (locationGranted && !backgroundLocationGranted &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    // Start/stop GPS tracking when trip is active
    LaunchedEffect(tripState.startedAt, tripState.destination) {
        if (tripState.startedAt != null && tripState.destination.isNotEmpty()) {
            liveTrainViewModel.startTracking(tripState.destination)
        } else {
            liveTrainViewModel.stopTracking()
        }
    }

    // Auto-pan: re-enable when a new trip begins
    val autoPanEnabled = remember { mutableStateOf(true) }
    LaunchedEffect(tripState.startedAt) {
        if (tripState.startedAt != null) autoPanEnabled.value = true
    }

    // Derived ETA — use live train position when available, fall back to origin
    val eta = when {
        trainState.inferredStationIndex >= 0 && mapDest.isNotEmpty() ->
            if (trainState.etaMinutes >= 0) trainState.etaMinutes else null
        mapOrigin.isNotEmpty() && mapDest.isNotEmpty() ->
            viewModel.calculateEta(mapOrigin, mapDest, statuses).takeIf { it >= 0 }
        else -> null
    }
    val worstStatus = if (mapOrigin.isNotEmpty() && mapDest.isNotEmpty())
        viewModel.getWorstStatus(mapOrigin, mapDest, statuses) else null

    // Overshoot detection
    val isOvershooting = remember(
        trainState.inferredStationIndex, tripState.origin, tripState.destination, trainState.signalLost
    ) {
        val destIdx  = STATION_NAMES.indexOf(tripState.destination)
        val origIdx  = STATION_NAMES.indexOf(tripState.origin)
        val trainIdx = trainState.inferredStationIndex
        if (destIdx < 0 || origIdx < 0 || trainIdx < 0 || trainState.signalLost) false
        else {
            val southbound = origIdx < destIdx
            if (southbound) trainIdx > destIdx else trainIdx < destIdx
        }
    }

    // Marker bitmaps (created once)
    val trainBitmap      = remember { createTrainMarkerBitmap() }
    val stationPinBitmap = remember { createStationPinBitmap() }

    // Stable MapView with drag-detection
    val mapView = remember {
        MapView(context).also { mv ->
            mv.addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent): Boolean {
                    autoPanEnabled.value = false
                    return false
                }
                override fun onZoom(event: ZoomEvent): Boolean = false
            })
        }
    }

    // Rebuild overlays on any relevant state change
    LaunchedEffect(statuses, locationGranted, mapOrigin, mapDest, trainState) {
        buildOsmOverlays(
            mapView, context, statuses, locationGranted,
            mapOrigin, mapDest, trainState, trainBitmap, stationPinBitmap
        )
    }

    // Auto-pan to train marker
    LaunchedEffect(trainState.inferredPosition) {
        val pos = trainState.inferredPosition ?: return@LaunchedEffect
        if (autoPanEnabled.value && tripState.startedAt != null) {
            mapView.controller.animateTo(GeoPoint(pos.latitude, pos.longitude))
        }
    }

    // Pause / resume / detach with Activity lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Device back button exits map lockout
    BackHandler { onExitMap() }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── OSMDroid map ──────────────────────────────────────────────────────
        AndroidView(
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 10.0
                    maxZoomLevel = 19.0
                    controller.apply {
                        setZoom(13.0)
                        setCenter(GeoPoint(14.5878, 121.0481))
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Top bar: back button + from/to pickers ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier  = Modifier.weight(1f),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                val displayOrigin = mapOrigin.ifEmpty { tripState.origin }
                val displayDest   = mapDest.ifEmpty   { tripState.destination }
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text     = displayOrigin.ifEmpty { "From station" },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showOriginPicker = true },
                            color    = if (displayOrigin.isEmpty()) TextSecondary else Color.Black,
                            maxLines = 1,
                            fontSize = 13.sp
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint     = PrimaryGreen,
                            modifier = Modifier.padding(horizontal = 4.dp).size(16.dp)
                        )
                        Text(
                            text     = displayDest.ifEmpty { "To station" },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDestPicker = true },
                            color    = if (displayDest.isEmpty()) TextSecondary else Color.Black,
                            maxLines = 1,
                            fontSize = 13.sp
                        )
                    }
                    if (tripState.fare > 0.0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Fare",
                                color    = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                "₱${"%.2f".format(tripState.fare)}",
                                color      = PrimaryGreen,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ── Signal lost tooltip ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = trainState.signalLost && tripState.startedAt != null,
            enter   = fadeIn(),
            exit    = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC333333)
            ) {
                Text(
                    "Signal lost — last known position shown",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color    = Color.White,
                    fontSize = 12.sp
                )
            }
        }

        // ── Overshoot warning banner ──────────────────────────────────────────
        AnimatedVisibility(
            visible  = isOvershooting,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 108.dp, start = 12.dp, end = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFEF4444)
            ) {
                Text(
                    "⚠ Check your stop — you may have passed your destination",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color    = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── OSM attribution (legally required) ───────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start  = 8.dp,
                    bottom = if (eta != null && worstStatus != null) 84.dp else 8.dp
                ),
            color = Color.White.copy(alpha = 0.75f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                "© OpenStreetMap contributors · Crowdsourced map data",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 9.sp,
                color    = Color(0xFF444444)
            )

        }

        // ── Bottom ETA / status card ──────────────────────────────────────────
        if (eta != null && worstStatus != null) {
            val (statusColor, statusText) = when (worstStatus) {
                StationStatus.CLEAR   -> Color(0xFF00A95C) to "Smooth Travel"
                StationStatus.SLIGHT  -> Color(0xFFFFC107) to "Minor Slowdown"
                StationStatus.HEAVY   -> Color(0xFFEF4444) to "Major Delays"
                StationStatus.OFFLINE -> Color.Black       to "Service Interrupted"
            }
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.BottomCenter),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(statusText, fontWeight = FontWeight.Bold, color = statusColor, fontSize = 15.sp)
                    }
                    val etaLabel = if (trainState.inferredStationIndex >= 0) "~$eta min (live)" else "~$eta min"
                    Surface(
                        color = if (eta >= 0) PrimaryGreen else Color.DarkGray,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            etaLabel,
                            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            color      = Color.White,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // ── Station picker sheets ─────────────────────────────────────────────────
    if (showOriginPicker) {
        StationPickerSheet(
            selectedStation   = mapOrigin,
            onStationSelected = { mapOrigin = it },
            onDismiss         = { showOriginPicker = false }
        )
    }
    if (showDestPicker) {
        StationPickerSheet(
            selectedStation   = mapDest,
            onStationSelected = { mapDest = it },
            onDismiss         = { showDestPicker = false }
        )
    }
}

// ── OSMDroid overlay builder ──────────────────────────────────────────────────

private fun buildOsmOverlays(
    mapView: MapView,
    context: Context,
    statuses: Map<String, String>,
    locationGranted: Boolean,
    selectedOrigin: String = "",
    selectedDest: String = "",
    trainState: LiveTrainViewModel.TrainState,
    trainBitmap: Bitmap,
    stationPinBitmap: Bitmap
) {
    mapView.overlays.clear()

    // User location blue dot
    if (locationGranted) {
        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
        locationOverlay.enableMyLocation()
        mapView.overlays.add(locationOverlay)
    }

    // Dark-green highlight for selected route (drawn below status lines)
    if (selectedOrigin.isNotEmpty() && selectedDest.isNotEmpty()) {
        val fromIdx = STATION_NAMES.indexOf(selectedOrigin)
        val toIdx   = STATION_NAMES.indexOf(selectedDest)
        if (fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx) {
            val start = minOf(fromIdx, toIdx); val end = maxOf(fromIdx, toIdx)
            val highlight = Polyline(mapView).apply {
                setPoints((start..end).map { i -> GeoPoint(MRT3_STATIONS[i].lat, MRT3_STATIONS[i].lng) })
                outlinePaint.color       = android.graphics.Color.parseColor("#FFD700")
                outlinePaint.strokeWidth = 22f
                outlinePaint.strokeCap   = Paint.Cap.ROUND
                outlinePaint.strokeJoin  = Paint.Join.ROUND
                outlinePaint.alpha       = 255
            }
            mapView.overlays.add(highlight)
        }
    }

    // Per-segment polylines colored by station status
    MRT3_STATIONS.zipWithNext().forEach { (a, b) ->
        val argbColor = when (stationStatusFromString(statuses[a.name] ?: "clear")) {
            StationStatus.CLEAR   -> android.graphics.Color.parseColor("#00A95C")
            StationStatus.SLIGHT  -> android.graphics.Color.parseColor("#FFC107")
            StationStatus.HEAVY   -> android.graphics.Color.parseColor("#EF4444")
            StationStatus.OFFLINE -> android.graphics.Color.BLACK
        }
        mapView.overlays.add(Polyline(mapView).apply {
            setPoints(listOf(GeoPoint(a.lat, a.lng), GeoPoint(b.lat, b.lng)))
            outlinePaint.color       = argbColor
            outlinePaint.strokeWidth = 10f
            outlinePaint.strokeCap   = Paint.Cap.ROUND
            outlinePaint.strokeJoin  = Paint.Join.ROUND
        })
    }

    // Station markers — classic location pin shape
    MRT3_STATIONS.forEach { station ->
        mapView.overlays.add(Marker(mapView).apply {
            position = GeoPoint(station.lat, station.lng)
            title    = station.name
            snippet  = "Status: ${statuses[station.name] ?: "clear"}"
            icon     = android.graphics.drawable.BitmapDrawable(context.resources, stationPinBitmap)
            // Anchor at the bottom tip of the pin so it points to the exact coordinate
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        })
    }

    // Live train marker (GPS-inferred position along the corridor)
    val trainPos = trainState.inferredPosition
    if (trainPos != null && !trainState.signalLost) {
        mapView.overlays.add(Marker(mapView).apply {
            position = GeoPoint(trainPos.latitude, trainPos.longitude)
            icon     = android.graphics.drawable.BitmapDrawable(context.resources, trainBitmap)
            title    = "Your position"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        })
    }

    mapView.invalidate()
}

// ── Station pin bitmap — classic teardrop location pin ────────────────────────
//
// Matches the standard map pin shape: rounded teardrop body pointing downward,
// with a hollow circle cutout in the upper half. Drawn in solid black to match
// the reference image supplied by the designer.
//
private fun createStationPinBitmap(): Bitmap {
    val w    = 48
    val h    = 64  // taller than wide so the tail has room
    val bmp  = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cvs  = Canvas(bmp)

    val cx   = w / 2f
    val r    = w / 2f          // radius of the circular head
    val headCenterY = r        // circle sits at the top

    // ── 1. Draw the teardrop body ─────────────────────────────────────────────
    // The body is built with a Path: two tangent lines from the circle down to
    // a pointed tip at the bottom center, then arced back across the top.
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.FILL
    }

    val path = Path().apply {
        // Tip of the pin at bottom center
        moveTo(cx, h.toFloat())
        // Left tangent line up to the circle
        lineTo(cx - r, headCenterY)
        // Arc across the top of the circle (left → right, clockwise)
        arcTo(
            cx - r, headCenterY - r, cx + r, headCenterY + r,
            180f, 180f, false
        )
        // Right tangent line back down to the tip
        lineTo(cx, h.toFloat())
        close()
    }
    cvs.drawPath(path, bodyPaint)

    // ── 2. Knock out the hollow circle in white ───────────────────────────────
    val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        // Use SRC_OVER; white circle punches through the black fill cleanly
    }
    val holeR = r * 0.42f   // ~42 % of the head radius — matches reference image
    cvs.drawCircle(cx, headCenterY, holeR, holePaint)

    return bmp
}

// ── Train marker bitmap (amber circle with "T" label) ─────────────────────────
private fun createTrainMarkerBitmap(): Bitmap {
    val size = 56
    val bmp  = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val cvs  = Canvas(bmp)

    // Glow ring
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FFCC44")
        alpha = 80
    }
    cvs.drawCircle(size / 2f, size / 2f, size / 2f, glowPaint)

    // Filled circle
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FF8C00")
    }
    cvs.drawCircle(size / 2f, size / 2f, size / 2.6f, fillPaint)

    // White border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    cvs.drawCircle(size / 2f, size / 2f, size / 2.6f, borderPaint)

    // "T" label
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = android.graphics.Color.WHITE
        textSize  = 18f
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.DEFAULT_BOLD
    }
    cvs.drawText("T", size / 2f, size / 2f + 7f, textPaint)

    return bmp
}