package com.mavaze.mygate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(
    entities = [
        User::class,
        Society::class,
        AppConfig::class,
        GateContact::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun societyDao(): SocietyDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun gateContactDao(): GateContactDao

    suspend fun createSocietyWithAdmin(
        society: Society,
        admin: User
    ): Long =
        withTransaction {
            val societyId = societyDao().insert(society)
            userDao().insert(
                admin.copy(societyId = societyId)
            )
            societyId
        }

    suspend fun deleteSocietyWithUsers(
        societyId: Long
    ) =
        withTransaction {
            userDao().deleteBySocietyId(societyId)
            gateContactDao().deleteForSociety(societyId)
            societyDao().deleteById(societyId)
        }

    companion object {
        private val MIGRATION_5_6 =
            object : androidx.room.migration.Migration(5, 6) {
                override fun migrate(
                    database: androidx.sqlite.db.SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        "ALTER TABLE gate_contacts ADD COLUMN alias TEXT"
                    )
                    database.execSQL(
                        "ALTER TABLE gate_contacts ADD COLUMN priority INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mygate.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
