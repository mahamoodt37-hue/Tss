package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ExtraCollectionDao
import com.example.data.dao.MemberDao
import com.example.data.dao.PaymentDao
import com.example.data.dao.TransferDao
import com.example.data.model.ExtraCollection
import com.example.data.model.Member
import com.example.data.model.PaymentRecord
import com.example.data.model.TransferRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Member::class,
        PaymentRecord::class,
        TransferRecord::class,
        ExtraCollection::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun paymentDao(): PaymentDao
    abstract fun transferDao(): TransferDao
    abstract fun extraCollectionDao(): ExtraCollectionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tss_fund_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // In case onCreate callback timing missed, ensure seed data exists
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        if (database.memberDao().getMemberCount() == 0) {
                            populateInitialData(database)
                        }
                    }
                }
            }

            suspend fun populateInitialData(database: AppDatabase) {
                val initialMembers = SeedData.getInitialMembers()
                database.memberDao().insertAll(initialMembers)
            }
        }
    }
}
