package com.example.data

import com.example.data.db.CodexEntity
import com.example.data.db.DefaultGameData
import com.example.data.db.GameDatabase
import com.example.data.db.MissionEntity
import com.example.data.db.PlayerProfile
import com.example.data.db.WeaponEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class GameRepository(private val database: GameDatabase) {
    private val playerDao = database.playerDao()
    private val missionDao = database.missionDao()
    private val weaponDao = database.weaponDao()
    private val codexDao = database.codexDao()

    val playerProfile: Flow<PlayerProfile?> = playerDao.getPlayerProfile()
    val allMissions: Flow<List<MissionEntity>> = missionDao.getAllMissions()
    val allWeapons: Flow<List<WeaponEntity>> = weaponDao.getAllWeapons()
    val allCodexEntries: Flow<List<CodexEntity>> = codexDao.getAllCodexEntries()

    suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        val current = playerDao.getPlayerProfile().firstOrNull()
        if (current == null) {
            playerDao.insertOrUpdate(DefaultGameData.initialProfile)
            missionDao.insertMissions(DefaultGameData.initialMissions)
            weaponDao.insertWeapons(DefaultGameData.initialWeapons)
            codexDao.insertCodexEntries(DefaultGameData.initialCodex)
        }
    }

    suspend fun updateEquippedWeapon(weaponId: String) = withContext(Dispatchers.IO) {
        val profile = playerDao.getPlayerProfile().firstOrNull() ?: DefaultGameData.initialProfile
        playerDao.update(profile.copy(equippedWeaponId = weaponId))
    }

    suspend fun upgradeWeapon(weaponId: String) = withContext(Dispatchers.IO) {
        val weapon = weaponDao.getWeaponById(weaponId) ?: return@withContext false
        val profile = playerDao.getPlayerProfile().firstOrNull() ?: return@withContext false
        val cost = (weapon.upgradeLevel * 1200)
        val naniteCost = (weapon.upgradeLevel * 50)
        if (profile.credits >= cost && profile.nanites >= naniteCost) {
            val updatedProfile = profile.copy(
                credits = profile.credits - cost,
                nanites = profile.nanites - naniteCost
            )
            playerDao.update(updatedProfile)

            val upgradedWeapon = weapon.copy(
                upgradeLevel = weapon.upgradeLevel + 1,
                baseDamage = weapon.baseDamage * 1.15f,
                magCapacity = weapon.magCapacity + (weapon.magCapacity / 6).coerceAtLeast(1)
            )
            weaponDao.updateWeapon(upgradedWeapon)
            true
        } else {
            false
        }
    }

    suspend fun unlockWeapon(weaponId: String) = withContext(Dispatchers.IO) {
        val weapon = weaponDao.getWeaponById(weaponId) ?: return@withContext false
        val profile = playerDao.getPlayerProfile().firstOrNull() ?: return@withContext false
        if (profile.credits >= weapon.unlockCost) {
            playerDao.update(profile.copy(credits = profile.credits - weapon.unlockCost))
            weaponDao.updateWeapon(weapon.copy(isUnlocked = true))
            true
        } else {
            false
        }
    }

    suspend fun recordMissionResult(
        missionId: String,
        score: Int,
        accuracy: Int,
        headshots: Int,
        kills: Int,
        stars: Int,
        victory: Boolean
    ) = withContext(Dispatchers.IO) {
        val mission = missionDao.getMissionById(missionId)
        val profile = playerDao.getPlayerProfile().firstOrNull() ?: DefaultGameData.initialProfile

        val earnedCredits = if (victory) (mission?.rewardCredits ?: 1000) else 400
        val earnedNanites = if (victory) (mission?.rewardNanites ?: 50) else 15
        val xpGain = (score / 10).coerceAtLeast(100)

        val newKills = profile.totalKills + kills
        val newHeadshots = profile.totalHeadshots + headshots
        val newHighestScore = maxOf(profile.highestScore, score)
        val newXp = profile.xp + xpGain
        val newLevel = 1 + (newXp / 1000)

        playerDao.update(
            profile.copy(
                credits = profile.credits + earnedCredits,
                nanites = profile.nanites + earnedNanites,
                xp = newXp,
                level = newLevel,
                totalKills = newKills,
                totalHeadshots = newHeadshots,
                highestScore = newHighestScore
            )
        )

        if (mission != null) {
            val newStars = maxOf(mission.starsEarned, stars)
            val updatedMission = mission.copy(
                isCompleted = mission.isCompleted || victory,
                starsEarned = newStars,
                bestScore = maxOf(mission.bestScore, score),
                bestAccuracy = maxOf(mission.bestAccuracy, accuracy)
            )
            missionDao.updateMission(updatedMission)

            // Unlock next mission if victory
            if (victory) {
                val all = DefaultGameData.initialMissions
                val currentIndex = all.indexOfFirst { it.id == missionId }
                if (currentIndex >= 0 && currentIndex + 1 < all.size) {
                    val nextId = all[currentIndex + 1].id
                    val nextMission = missionDao.getMissionById(nextId)
                    if (nextMission != null && !nextMission.isUnlocked) {
                        missionDao.updateMission(nextMission.copy(isUnlocked = true))
                    }
                }
            }
        }
    }
}
