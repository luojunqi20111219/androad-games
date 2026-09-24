package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun getPlayerProfile(): Flow<PlayerProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: PlayerProfile)

    @Update
    suspend fun update(profile: PlayerProfile)
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions ORDER BY act ASC, id ASC")
    fun getAllMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE id = :id LIMIT 1")
    suspend fun getMissionById(id: String): MissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<MissionEntity>)

    @Update
    suspend fun updateMission(mission: MissionEntity)
}

@Dao
interface WeaponDao {
    @Query("SELECT * FROM weapons")
    fun getAllWeapons(): Flow<List<WeaponEntity>>

    @Query("SELECT * FROM weapons WHERE id = :id LIMIT 1")
    suspend fun getWeaponById(id: String): WeaponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeapons(weapons: List<WeaponEntity>)

    @Update
    suspend fun updateWeapon(weapon: WeaponEntity)
}

@Dao
interface CodexDao {
    @Query("SELECT * FROM codex_entries")
    fun getAllCodexEntries(): Flow<List<CodexEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCodexEntries(entries: List<CodexEntity>)

    @Update
    suspend fun updateCodexEntry(entry: CodexEntity)
}
