package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TeamEntity
import com.example.viewmodel.BucketScoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMatchScreen(
    viewModel: BucketScoreViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLineupSetup: (Long) -> Unit
) {
    val teamsList by viewModel.teams.collectAsState()

    var homeTeamSelected by remember { mutableStateOf<TeamEntity?>(null) }
    var awayTeamSelected by remember { mutableStateOf<TeamEntity?>(null) }

    // Smart default date & venue
    val formattedDate = remember {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date())
    }
    var dateText by remember { mutableStateOf(formattedDate) }
    var venueText by remember { mutableStateOf("School Arena Gym") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Basketball Match", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Select Home Team",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            if (teamsList.isEmpty()) {
                Text(
                    text = "No teams available. Go back and create teams first!",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                ) {
                    items(teamsList) { team ->
                        val isSelected = homeTeamSelected?.id == team.id
                        val isExcluded = awayTeamSelected?.id == team.id

                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .clickable(enabled = !isExcluded) {
                                    homeTeamSelected = team
                                }
                                .testTag("home_team_select_${team.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isExcluded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                TeamLogo(logoUrl = team.logoUrl, emoji = team.logoEmoji, size = 32.dp, textSize = 20.sp)
                                Text(
                                    team.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Select Away Team",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            if (teamsList.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                ) {
                    items(teamsList) { team ->
                        val isSelected = awayTeamSelected?.id == team.id
                        val isExcluded = homeTeamSelected?.id == team.id

                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .clickable(enabled = !isExcluded) {
                                    awayTeamSelected = team
                                }
                                .testTag("away_team_select_${team.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.tertiaryContainer
                                    isExcluded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
                            } else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                TeamLogo(logoUrl = team.logoUrl, emoji = team.logoEmoji, size = 32.dp, textSize = 20.sp)
                                Text(
                                    team.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Logistics info
            Text(
                text = "Match Details",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Game Date") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("match_date_input")
            )

            OutlinedTextField(
                value = venueText,
                onValueChange = { venueText = it },
                label = { Text("Court / Arena Venue") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("match_venue_input")
            )

            Spacer(modifier = Modifier.weight(1f))

            val setupComplete = homeTeamSelected != null && awayTeamSelected != null && venueText.isNotBlank() && dateText.isNotBlank()

            Button(
                onClick = {
                    val homeId = homeTeamSelected?.id
                    val awayId = awayTeamSelected?.id
                    if (homeId != null && awayId != null) {
                        viewModel.createMatch(homeId, awayId, dateText, venueText) { matchId ->
                            onNavigateToLineupSetup(matchId)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_match_button"),
                shape = RoundedCornerShape(12.dp),
                enabled = setupComplete
            ) {
                Text("Lock Teams & Setup Lineups", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
