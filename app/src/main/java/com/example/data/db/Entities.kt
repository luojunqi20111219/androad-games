package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val callsign: String = "VANCE-01",
    val rankTitle: String = "星界上尉 (Captain)",
    val level: Int = 1,
    val xp: Int = 0,
    val credits: Int = 2500,
    val nanites: Int = 150,
    val totalKills: Int = 0,
    val totalHeadshots: Int = 0,
    val highestScore: Int = 0,
    val equippedWeaponId: String = "rifle_pulse"
)

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val act: Int,
    val title: String,
    val subtitle: String,
    val briefSummary: String,
    val targetType: String,
    val difficulty: String,
    val rewardCredits: Int,
    val rewardNanites: Int,
    val starsEarned: Int = 0,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val bestScore: Int = 0,
    val bestAccuracy: Int = 0
)

@Entity(tableName = "weapons")
data class WeaponEntity(
    @PrimaryKey val id: String,
    val name: String,
    val typeName: String,
    val description: String,
    val baseDamage: Float,
    val fireRateRps: Float,
    val magCapacity: Int,
    val reloadTimeSec: Float,
    val accuracySpread: Float,
    val isUnlocked: Boolean,
    val upgradeLevel: Int = 1,
    val unlockCost: Int = 0
)

@Entity(tableName = "codex_entries")
data class CodexEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val classification: String,
    val summary: String,
    val fullContent: String,
    val isUnlocked: Boolean = false
)
