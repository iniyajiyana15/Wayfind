package com.wayfind.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayfind.app.data.model.AccessibilityProfileType
import com.wayfind.app.ui.theme.AccentCyan
import com.wayfind.app.ui.theme.PrimaryBlue
import com.wayfind.app.ui.theme.Slate800
import com.wayfind.app.ui.theme.Slate900
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Visibility

@Composable
fun ProfileSelectionScreen(
    selectedProfile: AccessibilityProfileType,
    onSelectProfile: (AccessibilityProfileType) -> Unit,
    onConfirm: () -> Unit
) {
    val profiles = listOf(
        AccessibilityProfileType.WHEELCHAIR,
        AccessibilityProfileType.BLIND_LOW_VISION,
        AccessibilityProfileType.GENERAL
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Accessibility Profile",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )

            Text(
                text = "How should WAYFIND guide you?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Select your movement preferences to calculate adaptive routes.",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(profiles) { profile ->
                    val isSelected = profile == selectedProfile
                    val icon = when (profile) {
                        AccessibilityProfileType.WHEELCHAIR -> Icons.Default.Accessible
                        AccessibilityProfileType.BLIND_LOW_VISION -> Icons.Default.Visibility
                        else -> Icons.Default.DirectionsWalk
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProfile(profile) }
                            .then(
                                if (isSelected) Modifier.border(2.dp, PrimaryBlue, RoundedCornerShape(16.dp))
                                else Modifier
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Slate800 else Slate800.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = AccentCyan
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = profile.description,
                                    fontSize = 13.sp,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    profile.features.take(2).forEach { feature ->
                                        Surface(
                                            color = PrimaryBlue.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "• $feature",
                                                fontSize = 11.sp,
                                                color = AccentCyan,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = "Confirm & Explore Map",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
