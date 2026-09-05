package com.wayfind.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayfind.app.data.model.AnnouncementEvent
import com.wayfind.app.data.model.AnnouncementType
import com.wayfind.app.ui.theme.AccessibleGreen
import com.wayfind.app.ui.theme.HazardRed
import com.wayfind.app.ui.theme.WarningAmber

@Composable
fun AnnouncementBanner(
    announcement: AnnouncementEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = announcement != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        announcement?.let { event ->
            val bgColor = when (event.type) {
                AnnouncementType.ELEVATOR_FAILURE, AnnouncementType.HAZARD_DUMP_DETECTED -> HazardRed
                AnnouncementType.CLOSED_DOOR_AHEAD -> WarningAmber
                AnnouncementType.REROUTE_SUCCESS, AnnouncementType.DESTINATION_REACHED -> AccessibleGreen
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = bgColor,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = event.message,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
