package com.example.ui

import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.BucketScoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: BucketScoreViewModel,
    onAuthSuccess: () -> Unit,
    onNavigateToPublicScorecard: (Long) -> Unit
) {
    var showPublicCodeDialog by remember { mutableStateOf(false) }
    var matchCodeInput by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Auto routing on state update
    LaunchedEffect(viewModel.currentUser) {
        if (viewModel.currentUser != null) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Hero Banner Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_court),
                    contentDescription = "Basketball Court Hero Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Title overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "BucketScore",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Schools & Tournaments Live Tracker",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Auth card contents
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // App Logo Preview
                Image(
                    painter = painterResource(id = R.drawable.img_logo),
                    contentDescription = "BucketScore Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "Welcome to BucketScore!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Record matches live, track quarter scores, manage rosters, and instantly broadcast results to public viewers.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Error Display
                viewModel.authError?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Google sign in button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        // Use scorer email/name for simulated Google sign-in
                        viewModel.googleSignIn("scorer.google@school.edu", "Google Scorer", onAuthSuccess)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_auth_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏀  Continue with Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = " SPECTATE MATCH ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Open public scoreboard code button
                OutlinedButton(
                    onClick = { showPublicCodeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("public_scorecard_code_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "🔗  Enter Public Match Code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }

    // Public code entry alert dialog
    if (showPublicCodeDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPublicCodeDialog = false
                matchCodeInput = ""
                codeError = null
            },
            title = { Text("View Public Scorecard") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter the official match reference code (e.g., '1' or 'BS-1') to spectate the live scoreboard.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = matchCodeInput,
                        onValueChange = { 
                            matchCodeInput = it
                            codeError = null
                        },
                        label = { Text("Match Code") },
                        placeholder = { Text("e.g. BS-1") },
                        singleLine = true,
                        isError = codeError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (codeError != null) {
                        Text(
                            text = codeError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                val coroutineScope = rememberCoroutineScope()
                Button(
                    onClick = {
                        val inputTrimmed = matchCodeInput.trim()
                        if (inputTrimmed.isBlank()) {
                            codeError = "Please enter a match code."
                            return@Button
                        }
                        coroutineScope.launch {
                            val matchByCode = viewModel.getMatchByPublicCode(inputTrimmed)
                            if (matchByCode != null) {
                                showPublicCodeDialog = false
                                onNavigateToPublicScorecard(matchByCode.id)
                                matchCodeInput = ""
                                codeError = null
                                return@launch
                            }

                            val numericPart = inputTrimmed.replace("BS-", "", ignoreCase = true).trim()
                            val matchId = numericPart.toLongOrNull()
                            if (matchId != null) {
                                val matchById = viewModel.getMatchById(matchId)
                                if (matchById != null) {
                                    showPublicCodeDialog = false
                                    onNavigateToPublicScorecard(matchId)
                                    matchCodeInput = ""
                                    codeError = null
                                    return@launch
                                }
                            }

                            codeError = "Match code not found. Please try again."
                        }
                    }
                ) {
                    Text("Spectate")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPublicCodeDialog = false
                    matchCodeInput = ""
                    codeError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
