package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams ORDER BY name ASC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :id LIMIT 1")
    suspend fun getTeamById(id: Long): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity): Long
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY jerseyNumber ASC")
    fun getPlayersForTeam(teamId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE teamId = :teamId")
    suspend fun getPlayersForTeamList(teamId: Long): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id LIMIT 1")
    suspend fun getPlayerById(id: Long): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY id DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    suspend fun getMatchById(id: Long): MatchEntity?

    @Query("SELECT * FROM matches WHERE publicMatchCode = :code LIMIT 1")
    suspend fun getMatchByPublicCode(code: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)
}

@Dao
interface TimelineEventDao {
    @Query("SELECT * FROM timeline_events WHERE matchId = :matchId ORDER BY id DESC")
    fun getEventsForMatch(matchId: Long): Flow<List<TimelineEventEntity>>

    @Query("SELECT * FROM timeline_events WHERE matchId = :matchId")
    suspend fun getEventsForMatchList(matchId: Long): List<TimelineEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TimelineEventEntity)
}
