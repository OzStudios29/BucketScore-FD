package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val isGoogle: Boolean = false
)

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val logoEmoji: String = "🏀",
    val logoUrl: String? = null
)

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamId: Long,
    val name: String,
    val jerseyNumber: Int,
    val position: String,
    val photoUrl: String? = null
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val date: String,
    val venue: String,
    val isFinished: Boolean = false,
    val currentQuarter: Int = 1,
    val timerSecondsRemaining: Int = 600, // 10 minutes standard (600 seconds)
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val homeFouls: Int = 0,
    val awayFouls: Int = 0,
    // Comma-separated list of active player IDs on court (Starting 5 initially)
    val homeActivePlayerIds: String = "",
    val awayActivePlayerIds: String = "",
    // Quarter-by-quarter scores stored as comma-separated lists "0,0,0,0"
    val homeQuarterScores: String = "0,0,0,0",
    val awayQuarterScores: String = "0,0,0,0",
    val publicMatchCode: String = ""
)

@Entity(tableName = "timeline_events")
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val timestampText: String, // e.g. "08:12"
    val quarter: Int,
    val playerId: Long?,
    val playerName: String,
    val playerJersey: Int?,
    val teamId: Long,
    val teamName: String,
    val eventType: String, // "SCORE_1", "SCORE_2", "SCORE_3", "FOUL", "SUB_IN", "SUB_OUT"
    val pointsEffect: Int = 0,
    val detailText: String
)
