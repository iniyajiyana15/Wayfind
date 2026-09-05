package com.wayfind.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayfind.app.ui.theme.AccentCyan
import com.wayfind.app.ui.theme.PrimaryBlue
import com.wayfind.app.ui.theme.Slate800
import com.wayfind.app.ui.theme.Slate900

@Composable
fun WelcomeScreen(
    onStartNavigation: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Slate900, Slate800, Slate900)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon / Hero Badge
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = PrimaryBlue.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🧭", fontSize = 50.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "WAYFIND",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Navigate Without Barriers",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "\"Don't make people adapt to buildings. Make buildings adapt to people.\"",
                fontSize = 15.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Adaptive indoor navigation for everyone",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Start Navigation Button
            Button(
                onClick = onStartNavigation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = "Start Navigation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Bottom Simulated Banner
        Text(
            text = "SIMULATED BUILDING — PROTOTYPE DEMO",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
