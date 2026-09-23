package com.example.precord.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.precord.theme.PrecordPurple
import com.example.precord.theme.PrecordYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpgradeSheet(
    onDismiss: () -> Unit,
    onPurchased: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetMaxWidth = BottomSheetDefaults.SheetMaxWidth
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Star icon
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = PrecordYellow,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Unlock Pro Mode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Take your recordings to the next level",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feature list
            ProFeatureRow(Icons.Default.Timer, "Extended Recording", "Record up to 4 hours (free: 10 seconds)")
            ProFeatureRow(Icons.Default.Vibration, "Shake-to-Capture", "Shake your phone to save audio instantly")
            ProFeatureRow(Icons.Default.GraphicEq, "Smart Sound Detection", "Auto-bookmark when sound is detected")
            ProFeatureRow(Icons.Default.AutoFixHigh, "Audio Enhancement", "Noise reduction & volume normalization")
            ProFeatureRow(Icons.Default.Description, "Transcription", "Convert speech to text automatically")
            ProFeatureRow(Icons.Default.CloudUpload, "Cloud Backup", "Auto-upload captures to your cloud storage")

            Spacer(modifier = Modifier.height(32.dp))

            // Purchase button
            Button(
                onClick = onPurchased,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrecordPurple
                )
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = PrecordYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Unlock Pro — \$4.99 CAD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "One-time purchase • No subscription",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProFeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrecordYellow,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
