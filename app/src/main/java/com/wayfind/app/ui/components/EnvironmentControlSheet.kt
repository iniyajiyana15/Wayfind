package com.wayfind.app.ui.components

import androidx.compose.foundation.background
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
import com.wayfind.app.data.model.ClosedDoor
import com.wayfind.app.data.model.HazardDump
import com.wayfind.app.ui.theme.HazardRed
import com.wayfind.app.ui.theme.PrimaryBlue
import com.wayfind.app.ui.theme.Slate800
import com.wayfind.app.ui.theme.WarningAmber

@Composable
fun EnvironmentControlSheet(
    isElevatorE1Blocked: Boolean,
    isElevatorE2Blocked: Boolean,
    closedDoors: List<ClosedDoor>,
    hazardDumps: List<HazardDump>,
    onToggleElevatorE1: () -> Unit,
    onToggleElevatorE2: () -> Unit,
    onToggleDoor: (String) -> Unit,
    onToggleDump: (String) -> Unit,
    onRunDemo: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "🛠️ Developer & Environment Controls",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = "Simulate real-time architectural obstacles & elevator failures",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Demo Button: Simulate Elevator Failure
            Button(
                onClick = onToggleElevatorE1,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isElevatorE1Blocked) Color.Gray else HazardRed
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isElevatorE1Blocked) "✓ Restore Elevator E1" else "🚨 SIMULATE ELEVATOR E1 FAILURE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Elevator E2 Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Elevator E2 Operational Status", color = Color.White)
                Switch(
                    checked = !isElevatorE2Blocked,
                    onCheckedChange = { onToggleElevatorE2() }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)

            // Closed Doors Toggles
            Text(text = "🚪 Closed Door Barriers", fontWeight = FontWeight.Bold, color = WarningAmber)
            closedDoors.forEach { door ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(door.label, color = Color.White, fontSize = 14.sp)
                    Switch(
                        checked = door.isClosed,
                        onCheckedChange = { onToggleDoor(door.id) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)

            // Hazard Dumps Toggles
            Text(text = "⚠️ Debris / Dump Hazards", fontWeight = FontWeight.Bold, color = WarningAmber)
            hazardDumps.forEach { dump ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dump.label, color = Color.White, fontSize = 14.sp)
                    Switch(
                        checked = dump.isActive,
                        onCheckedChange = { onToggleDump(dump.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presentation Quick Demo Button
            OutlinedButton(
                onClick = onRunDemo,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🎬 Start 2-Min College Presentation Demo", color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close Control Panel", color = Color.LightGray)
            }
        }
    }
}
