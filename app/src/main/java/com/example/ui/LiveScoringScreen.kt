package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerEntity
import com.example.viewmodel.BucketScoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScoringScreen(
    matchId: Long,
    viewModel: BucketScoreViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSummary: (Long) -> Unit
) {
    // Load match on launch
    LaunchedEffect(matchId) {
        viewModel.loadActiveMatch(matchId)
    }

    val match = viewModel.activeMatch
    val homeTeam = viewModel.homeTeam
    val awayTeam = viewModel.awayTeam

    val timelineEvents by viewModel.activeMatchTimeline.collectAsState()

    var showEndMatchDialog by remember { mutableStateOf(false) }
    var showSubDialog by remember { mutableStateOf<PlayerEntity?>(null) } // Player to be subbed out
    var showShareDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Scoring Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.pauseTimer()
                        onNavigateBack()
                    }, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.testTag("share_match_appbar_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Match")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { showEndMatchDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("end_match_action_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End Match", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        if (match == null || homeTeam == null || awayTeam == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // --- SCOREBOARD PANEL ---
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    ),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Main scores & timer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Home score
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(homeTeam.logoEmoji, fontSize = 32.sp)
                                Text(
                                    homeTeam.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = match.homeScore.toString(),
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("home_score_display")
                                )
                                Text(
                                    text = "${match.homeFouls} Fouls",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (match.homeFouls >= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            if (match.homeFouls >= 5) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            // Center Timer Area
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "QUARTER ${match.currentQuarter}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Digital Clock display
                                Text(
                                    text = viewModel.formatTime(viewModel.timerSecondsRemaining),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (viewModel.timerSecondsRemaining <= 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("match_timer_display")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Timer controllers
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (viewModel.isTimerRunning) {
                                                viewModel.pauseTimer()
                                            } else {
                                                viewModel.startTimer()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (viewModel.isTimerRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                                shape = CircleShape
                                            )
                                            .testTag("play_pause_timer_button")
                                    ) {
                                        Icon(
                                            imageVector = if (viewModel.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Toggle Timer",
                                            tint = if (viewModel.isTimerRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (match.currentQuarter < 4) {
                                        IconButton(
                                            onClick = { viewModel.changeQuarter() },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = CircleShape
                                                )
                                                .testTag("next_quarter_button")
                                        ) {
                                            Icon(
                                                Icons.Default.SkipNext,
                                                contentDescription = "Next Quarter",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Away score
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(awayTeam.logoEmoji, fontSize = 32.sp)
                                Text(
                                    awayTeam.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = match.awayScore.toString(),
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("away_score_display")
                                )
                                Text(
                                    text = "${match.awayFouls} Fouls",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (match.awayFouls >= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            if (match.awayFouls >= 5) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MATCH CODE: ${match.publicMatchCode.ifBlank { "BS-${match.id}" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("match_code_display")
                            )

                            TextButton(
                                onClick = { showShareDialog = true },
                                modifier = Modifier.testTag("share_match_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Match", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- COURT ACTIVE PLAYERS GRID ---
                Text(
                    text = "SELECT ON-COURT PLAYER TO RECORD ACTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.3f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Home players column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                homeTeam.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(viewModel.homeActivePlayers) { player ->
                                val isSelected = viewModel.selectedPlayerForAction?.id == player.id
                                ActivePlayerCourtCard(
                                    player = player,
                                    isSelected = isSelected,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        viewModel.selectedPlayerForAction = player
                                        viewModel.selectedPlayerIsHome = true
                                    }
                                )
                            }
                        }
                    }

                    // Away players column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                awayTeam.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(viewModel.awayActivePlayers) { player ->
                                val isSelected = viewModel.selectedPlayerForAction?.id == player.id
                                ActivePlayerCourtCard(
                                    player = player,
                                    isSelected = isSelected,
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    onClick = {
                                        viewModel.selectedPlayerForAction = player
                                        viewModel.selectedPlayerIsHome = false
                                    }
                                )
                            }
                        }
                    }
                }

                // --- LIVE ACTION PERSISTENT CONTROLLER ---
                AnimatedVisibility(visible = viewModel.selectedPlayerForAction != null) {
                    viewModel.selectedPlayerForAction?.let { player ->
                        val isHome = viewModel.selectedPlayerIsHome
                        val accentColor = if (isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header: targeted player info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(accentColor, shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "#${player.jerseyNumber}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column {
                                            Text(
                                                player.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                if (isHome) homeTeam.name else awayTeam.name,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.selectedPlayerForAction = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close modifier", modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Quick action score points buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.recordScore(player, 1) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("score_1_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("+1 FT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.recordScore(player, 2) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("score_2_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("+2 PTS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.recordScore(player, 3) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("score_3_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("+3 PTS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                // Foul and substitution action buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.recordFoul(player) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .testTag("foul_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("FOUL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { showSubDialog = player },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("substitution_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SUB OUT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- LIVE FEED / TIMELINE LOGGER AT BOTTOM ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE TIMELINE LOGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (timelineEvents.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No scoring events recorded yet. Start clock and select a player to initiate tracking.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(timelineEvents, key = { it.id }) { ev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${ev.timestampText} Q${ev.quarter}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )

                                    // Display event content based on type
                                    val bubbleColor = when {
                                        ev.eventType.startsWith("SCORE") -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        ev.eventType == "FOUL" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                    val bubbleText = ev.detailText

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(bubbleColor)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = bubbleText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showShareDialog && match != null && homeTeam != null && awayTeam != null) {
            ShareMatchDialog(
                match = match,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                onDismiss = { showShareDialog = false }
            )
        }

        // End Match Dialog
        if (showEndMatchDialog) {
            AlertDialog(
                onDismissRequest = { showEndMatchDialog = false },
                title = { Text("End Match?") },
                text = { Text("Are you sure you want to finalize this basketball match? Scoring will lock, and the final summary sheet will compile.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showEndMatchDialog = false
                            viewModel.endMatch {
                                onNavigateToSummary(matchId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("dialog_end_match_confirm")
                    ) {
                        Text("Lock & Terminate")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndMatchDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Substitution dialog (Select entering player)
        showSubDialog?.let { activePlayer ->
            val isHome = viewModel.selectedPlayerIsHome
            val benchList = if (isHome) viewModel.homeBenchPlayers else viewModel.awayBenchPlayers

            AlertDialog(
                onDismissRequest = { showSubDialog = null },
                title = { Text("Substitute for ${activePlayer.name}") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Select a player from the squad bench to enter court play:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (benchList.isEmpty()) {
                            Text(
                                text = "Roster bench is empty! Register more players to support active substitutions.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            ) {
                                items(benchList) { bPlayer ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.executeSubstitution(activePlayer, bPlayer)
                                                showSubDialog = null
                                            }
                                            .testTag("dialog_sub_bench_player_${bPlayer.id}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("#${bPlayer.jerseyNumber}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            Text(bPlayer.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(bPlayer.position, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSubDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ActivePlayerCourtCard(
    player: PlayerEntity,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("court_player_card_${player.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, accentColor) else null
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(accentColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${player.jerseyNumber}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = player.position,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
