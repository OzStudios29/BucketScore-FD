package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BucketScoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BasketballRepository(AppDatabase.getDatabase(application))

    // --- Authentication ---
    var currentUser by mutableStateOf<UserEntity?>(null)
        private set

    var authError by mutableStateOf<String?>(null)
        private set

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authError = null
            val user = repository.authenticateUser(email, password)
            if (user != null) {
                currentUser = user
                onSuccess()
            } else {
                authError = "Invalid email or password"
            }
        }
    }

    fun signUp(email: String, name: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authError = null
            if (email.isBlank() || name.isBlank() || password.isBlank()) {
                authError = "All fields are required"
                return@launch
            }
            val success = repository.registerUser(email, name, password, isGoogle = false)
            if (success) {
                currentUser = UserEntity(email, name, password, isGoogle = false)
                onSuccess()
            } else {
                authError = "User with this email already exists"
            }
        }
    }

    fun googleSignIn(email: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authError = null
            val user = repository.authenticateWithGoogle(email, name)
            currentUser = user
            onSuccess()
        }
    }

    fun logout() {
        currentUser = null
        authError = null
    }

    // --- Teams & Players ---
    val teams = repository.allTeams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createTeam(name: String, emoji: String, logoUrl: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createTeam(name, emoji, logoUrl)
                onSuccess()
            }
        }
    }

    fun addPlayer(teamId: Long, name: String, jersey: Int, position: String, photoUrl: String? = null) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.addPlayer(teamId, name, jersey, position, photoUrl)
            }
        }
    }

    fun updatePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.updatePlayer(player)
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.deletePlayer(player)
        }
    }

    fun getPlayersForTeam(teamId: Long): Flow<List<PlayerEntity>> {
        return repository.getPlayersForTeam(teamId)
    }

    // --- Matches List ---
    val matches = repository.allMatches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createMatch(homeTeamId: Long, awayTeamId: Long, date: String, venue: String, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createMatch(homeTeamId, awayTeamId, date, venue)
            onSuccess(id)
        }
    }

    suspend fun getMatchByPublicCode(code: String): MatchEntity? {
        return repository.getMatchByPublicCode(code)
    }

    suspend fun getMatchById(id: Long): MatchEntity? {
        return repository.getMatchById(id)
    }

    // --- Live Active Match Scoring State ---
    var activeMatch by mutableStateOf<MatchEntity?>(null)
        private set

    var homeTeam by mutableStateOf<TeamEntity?>(null)
        private set

    var awayTeam by mutableStateOf<TeamEntity?>(null)
        private set

    var homePlayers by mutableStateOf<List<PlayerEntity>>(emptyList())
        private set

    var awayPlayers by mutableStateOf<List<PlayerEntity>>(emptyList())
        private set

    var homeActivePlayers by mutableStateOf<List<PlayerEntity>>(emptyList())
        private set

    var awayActivePlayers by mutableStateOf<List<PlayerEntity>>(emptyList())
        private set

    var homeBenchPlayers by mutableStateOf<List<PlayerEntity>>(emptyList())
        private set

    var awayBenchPlayers by mutableStateOf<List<PlayerEntity>>(emptyList())
        private set

    var timerSecondsRemaining by mutableStateOf(600)
    var isTimerRunning by mutableStateOf(false)
        private set

    var activeMatchTimeline = MutableStateFlow<List<TimelineEventEntity>>(emptyList())
        private set

    var selectedPlayerForAction by mutableStateOf<PlayerEntity?>(null)
    var selectedPlayerIsHome by mutableStateOf(true)

    private var timerJob: Job? = null

    fun loadActiveMatch(matchId: Long, onLoaded: () -> Unit = {}) {
        viewModelScope.launch {
            val match = repository.getMatchById(matchId)
            if (match != null) {
                activeMatch = match
                homeTeam = repository.getTeamById(match.homeTeamId)
                awayTeam = repository.getTeamById(match.awayTeamId)

                homePlayers = repository.getPlayersForTeamList(match.homeTeamId)
                awayPlayers = repository.getPlayersForTeamList(match.awayTeamId)

                timerSecondsRemaining = match.timerSecondsRemaining

                // Parse active players
                val homeActiveIds = match.homeActivePlayerIds.split(",").filter { it.isNotBlank() }.map { it.toLong() }
                val awayActiveIds = match.awayActivePlayerIds.split(",").filter { it.isNotBlank() }.map { it.toLong() }

                homeActivePlayers = homePlayers.filter { it.id in homeActiveIds }
                awayActivePlayers = awayPlayers.filter { it.id in awayActiveIds }

                homeBenchPlayers = homePlayers.filter { it.id !in homeActiveIds }
                awayBenchPlayers = awayPlayers.filter { it.id !in awayActiveIds }

                // Collect timeline in a separate coroutine so it doesn't block completion of this block
                launch {
                    repository.getEventsForMatch(matchId).collectLatest {
                        activeMatchTimeline.value = it
                    }
                }
            }
            onLoaded()
        }
    }

    fun startTimer() {
        if (isTimerRunning) return
        isTimerRunning = true
        timerJob = viewModelScope.launch {
            while (timerSecondsRemaining > 0 && isTimerRunning) {
                delay(1000)
                timerSecondsRemaining--
            }
            if (timerSecondsRemaining == 0) {
                isTimerRunning = false
                saveCurrentMatchState()
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning = false
        timerJob?.cancel()
        saveCurrentMatchState()
    }

    private fun saveCurrentMatchState() {
        val current = activeMatch ?: return
        viewModelScope.launch {
            val updated = current.copy(
                timerSecondsRemaining = timerSecondsRemaining,
                homeScore = current.homeScore,
                awayScore = current.awayScore,
                homeFouls = current.homeFouls,
                awayFouls = current.awayFouls,
                currentQuarter = current.currentQuarter,
                homeActivePlayerIds = homeActivePlayers.map { it.id }.joinToString(","),
                awayActivePlayerIds = awayActivePlayers.map { it.id }.joinToString(","),
                homeQuarterScores = current.homeQuarterScores,
                awayQuarterScores = current.awayQuarterScores
            )
            repository.updateMatch(updated)
            activeMatch = updated
        }
    }

    fun setupLineups(matchId: Long, homeStarterIds: List<Long>, awayStarterIds: List<Long>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val match = repository.getMatchById(matchId)
            if (match != null) {
                val updated = match.copy(
                    homeActivePlayerIds = homeStarterIds.joinToString(","),
                    awayActivePlayerIds = awayStarterIds.joinToString(",")
                )
                repository.updateMatch(updated)
                // Record kickoff timeline event
                val homeT = repository.getTeamById(match.homeTeamId)
                val awayT = repository.getTeamById(match.awayTeamId)
                repository.recordEvent(
                    TimelineEventEntity(
                        matchId = matchId,
                        timestampText = "10:00",
                        quarter = 1,
                        playerId = null,
                        playerName = "System",
                        playerJersey = null,
                        teamId = match.homeTeamId,
                        teamName = homeT?.name ?: "Home",
                        eventType = "QUARTER_START",
                        detailText = "Match started! Lineups locked."
                    )
                )
                onSuccess()
            }
        }
    }

    fun recordScore(player: PlayerEntity, points: Int) {
        val match = activeMatch ?: return
        val isHome = player.teamId == match.homeTeamId
        val timestamp = formatTime(timerSecondsRemaining)

        viewModelScope.launch {
            // Calculate scores
            val newHomeScore = if (isHome) match.homeScore + points else match.homeScore
            val newAwayScore = if (!isHome) match.awayScore + points else match.awayScore

            // Update quarter-by-quarter score
            val qIndex = match.currentQuarter - 1
            val hQuarters = match.homeQuarterScores.split(",").map { it.toInt() }.toMutableList()
            val aQuarters = match.awayQuarterScores.split(",").map { it.toInt() }.toMutableList()
            if (qIndex in 0..3) {
                if (isHome) {
                    hQuarters[qIndex] += points
                } else {
                    aQuarters[qIndex] += points
                }
            }

            val updatedMatch = match.copy(
                homeScore = newHomeScore,
                awayScore = newAwayScore,
                homeQuarterScores = hQuarters.joinToString(","),
                awayQuarterScores = aQuarters.joinToString(",")
            )

            // Insert Timeline Event
            val event = TimelineEventEntity(
                matchId = match.id,
                timestampText = timestamp,
                quarter = match.currentQuarter,
                playerId = player.id,
                playerName = player.name,
                playerJersey = player.jerseyNumber,
                teamId = player.teamId,
                teamName = if (isHome) (homeTeam?.name ?: "Home") else (awayTeam?.name ?: "Away"),
                eventType = "SCORE_$points",
                pointsEffect = points,
                detailText = "${player.name} (#${player.jerseyNumber}) scored $points point${if (points > 1) "s" else ""}."
            )

            repository.recordEvent(event)
            repository.updateMatch(updatedMatch)
            activeMatch = updatedMatch
            selectedPlayerForAction = null
        }
    }

    fun recordFoul(player: PlayerEntity) {
        val match = activeMatch ?: return
        val isHome = player.teamId == match.homeTeamId
        val timestamp = formatTime(timerSecondsRemaining)

        viewModelScope.launch {
            val newHomeFouls = if (isHome) match.homeFouls + 1 else match.homeFouls
            val newAwayFouls = if (!isHome) match.awayFouls + 1 else match.awayFouls

            val updatedMatch = match.copy(
                homeFouls = newHomeFouls,
                awayFouls = newAwayFouls
            )

            val event = TimelineEventEntity(
                matchId = match.id,
                timestampText = timestamp,
                quarter = match.currentQuarter,
                playerId = player.id,
                playerName = player.name,
                playerJersey = player.jerseyNumber,
                teamId = player.teamId,
                teamName = if (isHome) (homeTeam?.name ?: "Home") else (awayTeam?.name ?: "Away"),
                eventType = "FOUL",
                pointsEffect = 0,
                detailText = "${player.name} (#${player.jerseyNumber}) committed a foul."
            )

            repository.recordEvent(event)
            repository.updateMatch(updatedMatch)
            activeMatch = updatedMatch
            selectedPlayerForAction = null
        }
    }

    fun executeSubstitution(onCourtPlayer: PlayerEntity, benchPlayer: PlayerEntity) {
        val match = activeMatch ?: return
        val isHome = onCourtPlayer.teamId == match.homeTeamId
        val timestamp = formatTime(timerSecondsRemaining)

        val activeList = if (isHome) homeActivePlayers.toMutableList() else awayActivePlayers.toMutableList()
        val benchList = if (isHome) homeBenchPlayers.toMutableList() else awayBenchPlayers.toMutableList()

        if (activeList.remove(onCourtPlayer)) {
            benchList.add(onCourtPlayer)
        }
        if (benchList.remove(benchPlayer)) {
            activeList.add(benchPlayer)
        }

        if (isHome) {
            homeActivePlayers = activeList
            homeBenchPlayers = benchList
        } else {
            awayActivePlayers = activeList
            awayBenchPlayers = benchList
        }

        viewModelScope.launch {
            val updatedMatch = match.copy(
                homeActivePlayerIds = homeActivePlayers.map { it.id }.joinToString(","),
                awayActivePlayerIds = awayActivePlayers.map { it.id }.joinToString(",")
            )

            val event = TimelineEventEntity(
                matchId = match.id,
                timestampText = timestamp,
                quarter = match.currentQuarter,
                playerId = benchPlayer.id,
                playerName = benchPlayer.name,
                playerJersey = benchPlayer.jerseyNumber,
                teamId = benchPlayer.teamId,
                teamName = if (isHome) (homeTeam?.name ?: "Home") else (awayTeam?.name ?: "Away"),
                eventType = "SUB_IN",
                pointsEffect = 0,
                detailText = "Substitution: ${benchPlayer.name} (#${benchPlayer.jerseyNumber}) entered for ${onCourtPlayer.name} (#${onCourtPlayer.jerseyNumber})."
            )

            repository.recordEvent(event)
            repository.updateMatch(updatedMatch)
            activeMatch = updatedMatch
            selectedPlayerForAction = null
            saveCurrentMatchState()
        }
    }

    fun changeQuarter() {
        val match = activeMatch ?: return
        if (match.currentQuarter >= 4) return

        pauseTimer()
        val nextQuarter = match.currentQuarter + 1
        val timestamp = formatTime(timerSecondsRemaining)

        viewModelScope.launch {
            val updatedMatch = match.copy(
                currentQuarter = nextQuarter,
                timerSecondsRemaining = 600, // reset to 10 minutes
                homeFouls = 0, // team fouls reset in new quarter
                awayFouls = 0
            )

            val event = TimelineEventEntity(
                matchId = match.id,
                timestampText = timestamp,
                quarter = match.currentQuarter,
                playerId = null,
                playerName = "System",
                playerJersey = null,
                teamId = match.homeTeamId,
                teamName = homeTeam?.name ?: "Home",
                eventType = "QUARTER_START",
                detailText = "Quarter ${match.currentQuarter} ended. Quarter $nextQuarter started."
            )

            repository.recordEvent(event)
            repository.updateMatch(updatedMatch)
            activeMatch = updatedMatch
            timerSecondsRemaining = 600
        }
    }

    fun endMatch(onFinished: () -> Unit) {
        val match = activeMatch ?: return
        pauseTimer()
        val timestamp = formatTime(timerSecondsRemaining)

        viewModelScope.launch {
            val updatedMatch = match.copy(
                isFinished = true,
                timerSecondsRemaining = 0
            )

            val event = TimelineEventEntity(
                matchId = match.id,
                timestampText = timestamp,
                quarter = match.currentQuarter,
                playerId = null,
                playerName = "System",
                playerJersey = null,
                teamId = match.homeTeamId,
                teamName = homeTeam?.name ?: "Home",
                eventType = "MATCH_END",
                detailText = "Match finished! Final Score: ${homeTeam?.name} ${match.homeScore} - ${match.awayScore} ${awayTeam?.name}"
            )

            repository.recordEvent(event)
            repository.updateMatch(updatedMatch)
            activeMatch = updatedMatch
            onFinished()
        }
    }

    fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    // --- Seeding Default Mock Data ---
    fun seedDefaultDataIfEmpty() {
        viewModelScope.launch {
            // Wait for teams to load first or query DB directly
            val currentTeamsList = repository.getPlayersForTeamList(1L) // Check if team 1 has players
            val allTeamsFlowVal = teams.value
            if (allTeamsFlowVal.isEmpty()) {
                // Seed Team 1: Lions
                val lionsId = repository.createTeam("Lions", "🦁")
                repository.addPlayer(lionsId, "LeBron James", 23, "Small Forward")
                repository.addPlayer(lionsId, "Stephen Curry", 30, "Point Guard")
                repository.addPlayer(lionsId, "Kevin Durant", 35, "Power Forward")
                repository.addPlayer(lionsId, "Anthony Davis", 3, "Center")
                repository.addPlayer(lionsId, "Giannis Antetokounmpo", 34, "Shooting Guard")
                repository.addPlayer(lionsId, "Luka Doncic", 77, "Point Guard")
                repository.addPlayer(lionsId, "Kyrie Irving", 11, "Shooting Guard")

                // Seed Team 2: Eagles
                val eaglesId = repository.createTeam("Eagles", "🦅")
                repository.addPlayer(eaglesId, "Michael Jordan", 23, "Shooting Guard")
                repository.addPlayer(eaglesId, "Kobe Bryant", 24, "Shooting Guard")
                repository.addPlayer(eaglesId, "Shaquille O'Neal", 32, "Center")
                repository.addPlayer(eaglesId, "Larry Bird", 33, "Small Forward")
                repository.addPlayer(eaglesId, "Magic Johnson", 32, "Point Guard")
                repository.addPlayer(eaglesId, "Tim Duncan", 21, "Power Forward")
                repository.addPlayer(eaglesId, "Allen Iverson", 3, "Point Guard")

                // Seed Team 3: Warriors
                val warriorsId = repository.createTeam("Warriors", "⚡")
                repository.addPlayer(warriorsId, "Klay Thompson", 11, "Shooting Guard")
                repository.addPlayer(warriorsId, "Draymond Green", 23, "Power Forward")
                repository.addPlayer(warriorsId, "Steve Kerr", 25, "Point Guard")
                repository.addPlayer(warriorsId, "Andre Iguodala", 9, "Small Forward")
                repository.addPlayer(warriorsId, "Andrew Wiggins", 22, "Small Forward")
            }
        }
    }
}
