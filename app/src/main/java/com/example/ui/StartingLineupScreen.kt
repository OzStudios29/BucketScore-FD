package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerEntity
import com.example.viewmodel.BucketScoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartingLineupScreen(
    matchId: Long,
    viewModel: BucketScoreViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScoring: (Long) -> Unit
) {
    // Load active match in viewmodel
    LaunchedEffect(matchId) {
        viewModel.loadActiveMatch(matchId)
    }

    val match = viewModel.activeMatch
    val homeTeam = viewModel.homeTeam
    val awayTeam = viewModel.awayTeam

    // Gather and trigger player flow state
    val homePlayersFlow = remember(match?.homeTeamId) {
        match?.homeTeamId?.let { viewModel.getPlayersForTeam(it) }
    }
    val awayPlayersFlow = remember(match?.awayTeamId) {
        match?.awayTeamId?.let { viewModel.getPlayersForTeam(it) }
    }

    val homePlayersList by (homePlayersFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val awayPlayersList by (awayPlayersFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    val selectedHomePlayerIds = remember { mutableStateListOf<Long>() }
    val selectedAwayPlayerIds = remember { mutableStateListOf<Long>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Lineups", fontWeight = FontWeight.Bold) },
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
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                // Header overview card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TeamLogo(logoUrl = homeTeam.logoUrl, emoji = homeTeam.logoEmoji, size = 48.dp, textSize = 28.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(homeTeam.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Home • ${selectedHomePlayerIds.size}/5",
                                fontSize = 12.sp,
                                color = if (selectedHomePlayerIds.size == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text("VS", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TeamLogo(logoUrl = awayTeam.logoUrl, emoji = awayTeam.logoEmoji, size = 48.dp, textSize = 28.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(awayTeam.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Away • ${selectedAwayPlayerIds.size}/5",
                                fontSize = 12.sp,
                                color = if (selectedAwayPlayerIds.size == 5) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dual player rosters lists
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Home players selector (Left column)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${homeTeam.name} Squad (${homePlayersList.size}/8)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (homePlayersList.size < 8) {
                            RosterAutoSeedCard(
                                teamName = homeTeam.name,
                                currentCount = homePlayersList.size
                            ) {
                                // Add default players to reach 8
                                val needed = 8 - homePlayersList.size
                                repeat(needed) { idx ->
                                    val name = "Home Star ${homePlayersList.size + idx + 1}"
                                    val jersey = (homePlayersList.size + idx + 1) * 2
                                    val position = listOf("Point Guard", "Shooting Guard", "Small Forward", "Power Forward", "Center")[(homePlayersList.size + idx) % 5]
                                    viewModel.addPlayer(homeTeam.id, name, jersey, position)
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(homePlayersList) { player ->
                                    val isSelected = selectedHomePlayerIds.contains(player.id)
                                    PlayerSelectCard(
                                        player = player,
                                        isSelected = isSelected,
                                        activeColor = MaterialTheme.colorScheme.primaryContainer,
                                        onSelectedToggle = {
                                            if (isSelected) {
                                                selectedHomePlayerIds.remove(player.id)
                                            } else {
                                                if (selectedHomePlayerIds.size < 5) {
                                                    selectedHomePlayerIds.add(player.id)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Away players selector (Right column)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${awayTeam.name} Squad (${awayPlayersList.size}/8)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (awayPlayersList.size < 8) {
                            RosterAutoSeedCard(
                                teamName = awayTeam.name,
                                currentCount = awayPlayersList.size
                            ) {
                                // Add default players to reach 8
                                val needed = 8 - awayPlayersList.size
                                repeat(needed) { idx ->
                                    val name = "Away Star ${awayPlayersList.size + idx + 1}"
                                    val jersey = (awayPlayersList.size + idx + 1) * 3
                                    val position = listOf("Point Guard", "Shooting Guard", "Small Forward", "Power Forward", "Center")[(awayPlayersList.size + idx) % 5]
                                    viewModel.addPlayer(awayTeam.id, name, jersey, position)
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(awayPlayersList) { player ->
                                    val isSelected = selectedAwayPlayerIds.contains(player.id)
                                    PlayerSelectCard(
                                        player = player,
                                        isSelected = isSelected,
                                        activeColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        onSelectedToggle = {
                                            if (isSelected) {
                                                selectedAwayPlayerIds.remove(player.id)
                                            } else {
                                                if (selectedAwayPlayerIds.size < 5) {
                                                    selectedAwayPlayerIds.add(player.id)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val lineupsConfigured = homePlayersList.size >= 8 && awayPlayersList.size >= 8 && selectedHomePlayerIds.size == 5 && selectedAwayPlayerIds.size == 5

                Button(
                    onClick = {
                        if (lineupsConfigured) {
                            viewModel.setupLineups(matchId, selectedHomePlayerIds, selectedAwayPlayerIds) {
                                onNavigateToScoring(matchId)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("lock_lineups_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = lineupsConfigured
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lock Lineups & Jump Ball", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PlayerSelectCard(
    player: PlayerEntity,
    isSelected: Boolean,
    activeColor: Color,
    onSelectedToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectedToggle() }
            .testTag("player_card_${player.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerPhoto(photoUrl = player.photoUrl, size = 28.dp)

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("#${player.jerseyNumber}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(player.position, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun RosterAutoSeedCard(
    teamName: String,
    currentCount: Int,
    onSeedClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                text = "$teamName has only $currentCount player${if (currentCount != 1) "s" else ""}. Matches require at least 8.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onSeedClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Auto-fill to 8", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
