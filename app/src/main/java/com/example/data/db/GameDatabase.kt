package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PlayerProfile::class,
        MissionEntity::class,
        WeaponEntity::class,
        CodexEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun missionDao(): MissionDao
    abstract fun weaponDao(): WeaponDao
    abstract fun codexDao(): CodexDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getInstance(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "astral_vanguard.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            database.playerDao().insertOrUpdate(DefaultGameData.initialProfile)
                            database.missionDao().insertMissions(DefaultGameData.initialMissions)
                            database.weaponDao().insertWeapons(DefaultGameData.initialWeapons)
                            database.codexDao().insertCodexEntries(DefaultGameData.initialCodex)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
