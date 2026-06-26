package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MatchEntity
import com.example.data.TeamEntity

@Composable
fun ShareMatchDialog(
    match: MatchEntity,
    homeTeam: TeamEntity,
    awayTeam: TeamEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val matchCode = match.publicMatchCode.ifBlank { "BS-${match.id}" }
    val watchLink = "https://bucketscore.app/match/$matchCode"

    val fullMessage = """
        Follow the live BucketScore match:
        ${homeTeam.name} vs ${awayTeam.name}
        
        Match Code: $matchCode
        
        Watch live: $watchLink
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Share Live Match",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${homeTeam.logoEmoji} ${homeTeam.name} vs ${awayTeam.name} ${awayTeam.logoEmoji}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Match Code: $matchCode",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Option 1: Share Full Invitation
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, fullMessage)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Match")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Full Match Invitation")
                }

                // Option 2: Copy Code
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(matchCode))
                        Toast.makeText(context, "Match Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Match Code")
                }

                // Option 3: Copy Link
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(watchLink))
                        Toast.makeText(context, "Watch Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Watch Link")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
