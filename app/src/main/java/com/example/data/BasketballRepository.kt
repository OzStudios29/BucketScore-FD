package com.example.data

import kotlinx.coroutines.flow.Flow

class BasketballRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val teamDao = database.teamDao()
    private val playerDao = database.playerDao()
    private val matchDao = database.matchDao()
    private val timelineEventDao = database.timelineEventDao()

    // --- Authentication ---
    suspend fun registerUser(email: String, name: String, passwordHash: String, isGoogle: Boolean = false): Boolean {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) return false
        userDao.insertUser(UserEntity(email, name, passwordHash, isGoogle))
        return true
    }

    suspend fun authenticateUser(email: String, passwordHash: String): UserEntity? {
        val user = userDao.getUserByEmail(email)
        if (user != null && user.passwordHash == passwordHash && !user.isGoogle) {
            return user
        }
        return null
    }

    suspend fun authenticateWithGoogle(email: String, name: String): UserEntity {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) {
            return existing
        }
        val newUser = UserEntity(email = email, name = name, passwordHash = "GOOGLE_AUTH_TOKEN", isGoogle = true)
        userDao.insertUser(newUser)
        return newUser
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    // --- Teams ---
    val allTeams: Flow<List<TeamEntity>> = teamDao.getAllTeams()

    suspend fun getTeamById(id: Long): TeamEntity? = teamDao.getTeamById(id)

    suspend fun createTeam(name: String, logoEmoji: String, logoUrl: String? = null): Long {
        return teamDao.insertTeam(TeamEntity(name = name, logoEmoji = logoEmoji, logoUrl = logoUrl))
    }

    suspend fun updateTeam(team: TeamEntity) {
        teamDao.insertTeam(team)
    }

    // --- Players ---
    fun getPlayersForTeam(teamId: Long): Flow<List<PlayerEntity>> = playerDao.getPlayersForTeam(teamId)

    suspend fun getPlayersForTeamList(teamId: Long): List<PlayerEntity> = playerDao.getPlayersForTeamList(teamId)

    suspend fun getPlayerById(id: Long): PlayerEntity? = playerDao.getPlayerById(id)

    suspend fun addPlayer(teamId: Long, name: String, jerseyNumber: Int, position: String, photoUrl: String? = null): Long {
        return playerDao.insertPlayer(PlayerEntity(teamId = teamId, name = name, jerseyNumber = jerseyNumber, position = position, photoUrl = photoUrl))
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        playerDao.updatePlayer(player)
    }

    suspend fun deletePlayer(player: PlayerEntity) {
        playerDao.deletePlayer(player)
    }

    // --- Matches ---
    val allMatches: Flow<List<MatchEntity>> = matchDao.getAllMatches()

    suspend fun getMatchById(id: Long): MatchEntity? = matchDao.getMatchById(id)

    suspend fun getMatchByPublicCode(code: String): MatchEntity? = matchDao.getMatchByPublicCode(code)

    suspend fun createMatch(homeTeamId: Long, awayTeamId: Long, date: String, venue: String): Long {
        var generatedCode = ""
        var isUnique = false
        while (!isUnique) {
            val randomNum = (1000..9999).random()
            generatedCode = "BS-$randomNum"
            val existing = matchDao.getMatchByPublicCode(generatedCode)
            if (existing == null) {
                isUnique = true
            }
        }

        return matchDao.insertMatch(MatchEntity(
            homeTeamId = homeTeamId,
            awayTeamId = awayTeamId,
            date = date,
            venue = venue,
            isFinished = false,
            publicMatchCode = generatedCode
        ))
    }

    suspend fun updateMatch(match: MatchEntity) {
        matchDao.updateMatch(match)
    }

    // --- Timeline & Events ---
    fun getEventsForMatch(matchId: Long): Flow<List<TimelineEventEntity>> = timelineEventDao.getEventsForMatch(matchId)

    suspend fun getEventsForMatchList(matchId: Long): List<TimelineEventEntity> = timelineEventDao.getEventsForMatchList(matchId)

    suspend fun recordEvent(event: TimelineEventEntity) {
        timelineEventDao.insertEvent(event)
    }
}
