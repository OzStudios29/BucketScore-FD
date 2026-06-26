package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
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
fun PublicScorecardScreen(
    matchId: Long,
    viewModel: BucketScoreViewModel,
    onNavigateBack: () -> Unit
) {
    // Load match details on launch
    LaunchedEffect(matchId) {
        viewModel.loadActiveMatch(matchId)
    }

    val match = viewModel.activeMatch
    val homeTeam = viewModel.homeTeam
    val awayTeam = viewModel.awayTeam
    val timelineEvents by viewModel.activeMatchTimeline.collectAsState()
    var showShareDialog by remember { mutableStateOf(false) }

    // Aggregate Box Score Stats from Play-by-Play Events
    val playerStatsMap = remember(timelineEvents) {
        val map = mutableMapOf<Long, Pair<Int, Int>>() // playerId -> (Points, Fouls)
        timelineEvents.forEach { ev ->
            val pId = ev.playerId ?: return@forEach
            val current = map.getOrDefault(pId, Pair(0, 0))
            var pointsAdd = 0
            var foulsAdd = 0
            if (ev.eventType.startsWith("SCORE_")) {
                pointsAdd = ev.pointsEffect
            } else if (ev.eventType == "FOUL") {
                foulsAdd = 1
            }
            map[pId] = Pair(current.first + pointsAdd, current.second + foulsAdd)
        }
        map
    }

    // Find top scorer
    val topScorerIdAndStats = playerStatsMap.entries.maxByOrNull { it.value.first }
    val topScorerEvent = timelineEvents.firstOrNull { it.playerId == topScorerIdAndStats?.key }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Public Game Scorecard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text("Loading live scorecard data...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scorecard Badge Code
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔗  PUBLIC MATCH CODE: ${match.publicMatchCode.ifBlank { "BS-${match.id}" }}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.testTag("match_code_display")
                        )

                        TextButton(
                            onClick = { showShareDialog = true },
                            modifier = Modifier.testTag("share_match_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // --- HEADER: SCORES CARD ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "OFFICIAL LIVE BROADCAST SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    TeamLogo(logoUrl = homeTeam.logoUrl, emoji = homeTeam.logoEmoji, size = 48.dp, textSize = 32.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(homeTeam.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = match.homeScore.toString(),
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (match.isFinished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (match.isFinished) "FINAL" else "LIVE",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    TeamLogo(logoUrl = awayTeam.logoUrl, emoji = awayTeam.logoEmoji, size = 48.dp, textSize = 32.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(awayTeam.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = match.awayScore.toString(),
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                            Text(
                                text = "Venue: ${match.venue} | Date: ${match.date}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // --- QUARTER BREAKDOWN CHART ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "QUARTER-WISE SCOREBOARD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val homeQuarters = match.homeQuarterScores.split(",").map { it.toIntOrNull() ?: 0 }
                            val awayQuarters = match.awayQuarterScores.split(",").map { it.toIntOrNull() ?: 0 }

                            // Table header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TEAM", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                                Text("Q1", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("Q2", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("Q3", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("Q4", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("TOT", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }

                            // Home row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(homeTeam.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${homeQuarters.getOrElse(0) { 0 }}", fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("${homeQuarters.getOrElse(1) { 0 }}", fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("${homeQuarters.getOrElse(2) { 0 }}", fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("${homeQuarters.getOrElse(3) { 0 }}", fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("${match.homeScore}", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.primary)
                            }

                            HorizontalDivider()

                            // Away row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(awayTeam.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${awayQuarters.getOrElse(0) { 0 }}", fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("${awayQuarters.getOrElse(1) { 0 }}", fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("${awayQuarters.getOrElse(2) { 0 }}", fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("${awayQuarters.getOrElse(3) { 0 }}", fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("${match.awayScore}", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }

                // --- TOP SCORER SPOTLIGHT ---
                if (topScorerIdAndStats != null && topScorerEvent != null && topScorerIdAndStats.value.first > 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(MaterialTheme.colorScheme.tertiary, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👑", fontSize = 28.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "GAME MVP / TOP SCORER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "${topScorerEvent.playerName} (#${topScorerEvent.playerJersey})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = topScorerEvent.teamName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${topScorerIdAndStats.value.first}",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text("POINTS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // --- BOX SCORE: PLAYER BOX STATISTICS ---
                item {
                    Text(
                        text = "PLAYER DETAILED BOX SCORES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Home Squad Stats
                item {
                    BoxScoreGroupReadOnly(
                        teamName = homeTeam.name,
                        players = viewModel.homePlayers,
                        statsMap = playerStatsMap,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                }

                // Away Squad Stats
                item {
                    BoxScoreGroupReadOnly(
                        teamName = awayTeam.name,
                        players = viewModel.awayPlayers,
                        statsMap = playerStatsMap,
                        accentColor = MaterialTheme.colorScheme.tertiary
                    )
                }

                // --- PLAY-BY-PLAY CHRONOLOGICAL TIMELINE ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = "CHRONOLOGICAL GAME TIMELINE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (timelineEvents.isEmpty()) {
                    item {
                        Text(
                            "No events recorded in this match timeline yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(timelineEvents.reversed()) { ev ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${ev.timestampText} Q${ev.quarter}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Text(
                                    text = ev.detailText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
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
    }
}

@Composable
fun BoxScoreGroupReadOnly(
    teamName: String,
    players: List<PlayerEntity>,
    statsMap: Map<Long, Pair<Int, Int>>,
    accentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = teamName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            // Header labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("PLAYER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
                Text("PTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("FOULS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            // Player rows
            if (players.isEmpty()) {
                Text(
                    "No players registered in roster.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                players.forEach { p ->
                    val stats = statsMap.getOrDefault(p.id, Pair(0, 0))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
                            PlayerPhoto(photoUrl = p.photoUrl, size = 24.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Text("${stats.first}", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("${stats.second}", fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}
