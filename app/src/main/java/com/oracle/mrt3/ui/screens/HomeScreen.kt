package com.oracle.mrt3.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oracle.mrt3.R
import com.oracle.mrt3.ui.theme.PrimaryGreen
import com.oracle.mrt3.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onNavigateToFare: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToEmergency: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F0))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        Image(
            painter = painterResource(R.drawable.icon),
            contentDescription = "Oracle Logo",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(14.dp))
        Text(
            text = "ORACLE",
            color = PrimaryGreen,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 5.sp
        )
        Text(
            text = "MRT-3 COMMUTER GUIDE",
            color = TextSecondary,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(36.dp))

        ImageFeatureCard(
            title       = "Fare Matrix",
            subtitle    = "Calculate your MRT-3 fare",
            imageRes    = R.drawable.fare,
            accentColor = Color(0xFF00703C),
            onClick     = onNavigateToFare
        )
        Spacer(Modifier.height(14.dp))
        ImageFeatureCard(
            title       = "Map",
            subtitle    = "View stations & track your route",
            imageRes    = R.drawable.mapping,
            accentColor = Color(0xFF0D47A1),
            onClick     = onNavigateToMap
        )
        Spacer(Modifier.height(14.dp))
        ImageFeatureCard(
            title       = "Emergency",
            subtitle    = "Report incidents & get help",
            imageRes    = R.drawable.emergency,
            accentColor = Color(0xFFC62828),
            onClick     = onNavigateToEmergency
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ImageFeatureCard(
    title: String,
    subtitle: String,
    imageRes: Int,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to accentColor.copy(alpha = 0.92f),
                            0.55f to accentColor.copy(alpha = 0.55f),
                            1.0f to Color.Transparent
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 22.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp
                )
            }
        }
    }
}
