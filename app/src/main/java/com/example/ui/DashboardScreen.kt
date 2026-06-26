package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.MatchEntity
import com.example.data.TeamEntity
import com.example.data.PlayerEntity
import com.example.data.ImageStorageManager
import com.example.viewmodel.BucketScoreViewModel
import java.io.File
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BucketScoreViewModel,
    onNavigateToCreateTeam: () -> Unit,
    onNavigateToCreateMatch: () -> Unit,
    onNavigateToLineup: (Long) -> Unit,
    onNavigateToScoring: (Long) -> Unit,
    onNavigateToSummary: (Long) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Matches, 1 = Teams
    val teamsList by viewModel.teams.collectAsState()
    val matchesList by viewModel.matches.collectAsState()

    // Trigger seed logic on launch if empty
    LaunchedEffect(Unit) {
        viewModel.seedDefaultDataIfEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_logo),
                            contentDescription = "BucketScore Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Column {
                            Text(
                                "BucketScore",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                "Live Tournament Scorer",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Log Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToCreateMatch,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Match") },
                    modifier = Modifier.testTag("new_match_fab")
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToCreateTeam,
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                    text = { Text("New Team") },
                    modifier = Modifier.testTag("new_team_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Logged in scorer tag
            viewModel.currentUser?.let { user ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Logged in as ${user.name} (${user.email})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Tabs Selector
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.SportsBasketball, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Matches", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("matches_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Teams", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("teams_tab")
                )
            }

            // Demo seeding helper if nothing exists
            if (teamsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.seedDefaultDataIfEmpty() }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🧪", fontSize = 40.sp)
                        Text(
                            "Seed Demo Basketball Data",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Click here to instantly pre-populate the app with professional mock teams (Lions, Eagles, Warriors) and dynamic player rosters for quick scoring!",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Content based on Tab
            if (selectedTab == 0) {
                MatchesListSection(
                    matches = matchesList,
                    teams = teamsList,
                    onNavigateToLineup = onNavigateToLineup,
                    onNavigateToScoring = onNavigateToScoring,
                    onNavigateToSummary = onNavigateToSummary
                )
            } else {
                TeamsListSection(
                    teams = teamsList,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun MatchesListSection(
    matches: List<MatchEntity>,
    teams: List<TeamEntity>,
    onNavigateToLineup: (Long) -> Unit,
    onNavigateToScoring: (Long) -> Unit,
    onNavigateToSummary: (Long) -> Unit
) {
    if (matches.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.SportsBasketball,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Text(
                    text = "No Matches Yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap 'New Match' at the bottom right to start record-keeping for an upcoming school game.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(matches, key = { it.id }) { match ->
                val home = teams.firstOrNull { it.id == match.homeTeamId }
                val away = teams.firstOrNull { it.id == match.awayTeamId }

                val isSetupNeeded = match.homeActivePlayerIds.isBlank() && !match.isFinished

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (match.isFinished) {
                                onNavigateToSummary(match.id)
                            } else if (isSetupNeeded) {
                                onNavigateToLineup(match.id)
                            } else {
                                onNavigateToScoring(match.id)
                            }
                        }
                        .testTag("match_card_${match.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header Status Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${match.date} • ${match.venue}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )

                            // Status badge
                            val badgeColor = when {
                                match.isFinished -> MaterialTheme.colorScheme.secondary
                                isSetupNeeded -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error // Live red
                            }
                            val badgeText = when {
                                match.isFinished -> "FINAL"
                                isSetupNeeded -> "LINEUP REQ"
                                else -> "LIVE - Q${match.currentQuarter}"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Teams Grid Matchup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Home Team
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(home?.logoEmoji ?: "🏀", fontSize = 18.sp)
                                }
                                Text(
                                    text = home?.name ?: "Home Team",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Score / Versus Banner
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSetupNeeded) {
                                    Text(
                                        text = "VS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "${match.homeScore} - ${match.awayScore}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Away Team
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = away?.name ?: "Away Team",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(away?.logoEmoji ?: "🏀", fontSize = 18.sp)
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        )

                        // Action Guide Text
                        val actionHint = when {
                            match.isFinished -> "View match summary and player statistics"
                            isSetupNeeded -> "Tap to lock starting 5 and initiate kickoff"
                            else -> "Tap to resume active match record-keeping"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (match.isFinished) Icons.Default.BarChart else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = actionHint,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamsListSection(
    teams: List<TeamEntity>,
    viewModel: BucketScoreViewModel
) {
    if (teams.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Text(
                    text = "No Teams Registered",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap 'New Team' below to register team names, logos, and assign squad players with jersey numbers.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(teams, key = { it.id }) { team ->
                var isExpanded by remember { mutableStateOf(false) }
                val playersFlow = remember(team.id) { viewModel.getPlayersForTeam(team.id) }
                val players by playersFlow.collectAsState(initial = emptyList())

                var showAddPlayerDialog by remember { mutableStateOf(false) }
                var playerToEdit by remember { mutableStateOf<PlayerEntity?>(null) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("team_card_${team.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TeamLogo(logoUrl = team.logoUrl, emoji = team.logoEmoji, size = 44.dp, textSize = 24.sp)

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = team.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${players.size} Players registered",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { isExpanded = !isExpanded }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand players"
                                )
                            }
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ROSTER / PLAYERS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Button(
                                    onClick = { showAddPlayerDialog = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("add_player_button_${team.id}"),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Player", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (players.isEmpty()) {
                                Text(
                                    text = "No players registered in this team squad yet. Please tap 'Add Player' to add a starting 5.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    players.forEach { p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.5f
                                                    ),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PlayerPhoto(photoUrl = p.photoUrl, size = 32.dp)
                                            Spacer(modifier = Modifier.width(8.dp))

                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "#${p.jerseyNumber}",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = p.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = p.position,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))

                                            IconButton(
                                                onClick = { playerToEdit = p },
                                                modifier = Modifier.size(28.dp).testTag("edit_player_${p.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit Player",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.deletePlayer(p) },
                                                modifier = Modifier.size(28.dp).testTag("delete_player_${p.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Player",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Player dialog
                if (showAddPlayerDialog) {
                    AddPlayerDialog(
                        teamName = team.name,
                        onDismiss = { showAddPlayerDialog = false },
                        onConfirm = { pName, pJersey, pPos, pPhoto ->
                            viewModel.addPlayer(team.id, pName, pJersey, pPos, pPhoto)
                            showAddPlayerDialog = false
                        }
                    )
                }

                // Edit Player dialog
                if (playerToEdit != null) {
                    EditPlayerDialog(
                        player = playerToEdit!!,
                        onDismiss = { playerToEdit = null },
                        onConfirm = { pName, pJersey, pPos, pPhoto ->
                            viewModel.updatePlayer(playerToEdit!!.copy(name = pName, jerseyNumber = pJersey, position = pPos, photoUrl = pPhoto))
                            playerToEdit = null
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerDialog(
    teamName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var jerseyText by remember { mutableStateOf("") }
    var selectedPosition by remember { mutableStateOf("Point Guard") }

    // Media support states
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var uploadedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                val resultUrl = ImageStorageManager.saveAndUploadImage(context, uri, "players")
                uploadedPhotoUrl = resultUrl
                isUploading = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.let { file ->
                coroutineScope.launch {
                    isUploading = true
                    val resultUrl = ImageStorageManager.saveAndUploadImage(context, android.net.Uri.fromFile(file), "players")
                    uploadedPhotoUrl = resultUrl
                    isUploading = false
                }
            }
        }
    }

    val positions = listOf("Point Guard", "Shooting Guard", "Small Forward", "Power Forward", "Center")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Player to $teamName") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                // Profile photo preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (!uploadedPhotoUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = uploadedPhotoUrl,
                            contentDescription = "Player Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                // Photo selection buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val file = ImageStorageManager.createTempImageFile(context)
                            tempCameraFile = file
                            val cameraUri = ImageStorageManager.getUriForFile(context, file)
                            cameraLauncher.launch(cameraUri)
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_player_name")
                )

                OutlinedTextField(
                    value = jerseyText,
                    onValueChange = { jerseyText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Jersey Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_player_jersey")
                )

                Text(
                    text = "Tactical Position",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Simple flow layout or row of position pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    positions.chunked(2).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { pos ->
                                FilterChip(
                                    selected = selectedPosition == pos,
                                    onClick = { selectedPosition = pos },
                                    label = { Text(pos, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val jersey = jerseyText.toIntOrNull() ?: 0
                    if (name.isNotBlank()) {
                        onConfirm(name, jersey, selectedPosition, uploadedPhotoUrl)
                    }
                },
                modifier = Modifier.testTag("dialog_confirm_player"),
                enabled = !isUploading
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlayerDialog(
    player: PlayerEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(player.name) }
    var jerseyText by remember { mutableStateOf(player.jerseyNumber.toString()) }
    var selectedPosition by remember { mutableStateOf(player.position) }

    // Media support states
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var uploadedPhotoUrl by remember { mutableStateOf(player.photoUrl) }
    var isUploading by remember { mutableStateOf(false) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                val resultUrl = ImageStorageManager.saveAndUploadImage(context, uri, "players")
                uploadedPhotoUrl = resultUrl
                isUploading = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.let { file ->
                coroutineScope.launch {
                    isUploading = true
                    val resultUrl = ImageStorageManager.saveAndUploadImage(context, android.net.Uri.fromFile(file), "players")
                    uploadedPhotoUrl = resultUrl
                    isUploading = false
                }
            }
        }
    }

    val positions = listOf("Point Guard", "Shooting Guard", "Small Forward", "Power Forward", "Center")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Player Profile") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                // Profile photo preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (!uploadedPhotoUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = uploadedPhotoUrl,
                            contentDescription = "Player Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                // Photo selection buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val file = ImageStorageManager.createTempImageFile(context)
                            tempCameraFile = file
                            val cameraUri = ImageStorageManager.getUriForFile(context, file)
                            cameraLauncher.launch(cameraUri)
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_player_name")
                )

                OutlinedTextField(
                    value = jerseyText,
                    onValueChange = { jerseyText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Jersey Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_edit_player_jersey")
                )

                Text(
                    text = "Tactical Position",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Simple flow layout or row of position pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    positions.chunked(2).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { pos ->
                                FilterChip(
                                    selected = selectedPosition == pos,
                                    onClick = { selectedPosition = pos },
                                    label = { Text(pos, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val jersey = jerseyText.toIntOrNull() ?: 0
                    if (name.isNotBlank()) {
                        onConfirm(name, jersey, selectedPosition, uploadedPhotoUrl)
                    }
                },
                modifier = Modifier.testTag("dialog_edit_confirm_player"),
                enabled = !isUploading
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
