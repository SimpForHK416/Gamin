package com.example.gamin.Arkanoid.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery

// === Entity: Định nghĩa bảng High Score (Đã thêm Unique Index) ===
@Entity(
    tableName = "high_scores",
    // Khai báo rằng tổ hợp của 10 cột score phải là DUY NHẤT
    indices = [Index(
        value = ["wave_1_score", "wave_2_score", "wave_3_score", "wave_4_score", "wave_5_score",
            "wave_6_score", "wave_7_score", "wave_8_score", "wave_9_score", "wave_10_score"],
        unique = true
    )]
)
data class ScoreRecord(
    // Khóa chính tự động tăng
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // Thời gian lưu record
    @ColumnInfo(name = "date") val timestamp: Long = System.currentTimeMillis(),

    // Lưu điểm cho 10 màn chơi (Bỏ cột Time)
    @ColumnInfo(name = "wave_1_score") val wave1Score: Int = 0,
    @ColumnInfo(name = "wave_2_score") val wave2Score: Int = 0,
    @ColumnInfo(name = "wave_3_score") val wave3Score: Int = 0,
    @ColumnInfo(name = "wave_4_score") val wave4Score: Int = 0,
    @ColumnInfo(name = "wave_5_score") val wave5Score: Int = 0,
    @ColumnInfo(name = "wave_6_score") val wave6Score: Int = 0,
    @ColumnInfo(name = "wave_7_score") val wave7Score: Int = 0,
    @ColumnInfo(name = "wave_8_score") val wave8Score: Int = 0,
    @ColumnInfo(name = "wave_9_score") val wave9Score: Int = 0,
    @ColumnInfo(name = "wave_10_score") val wave10Score: Int = 0,
)

// === Data Access Object (DAO) ===
@Dao
interface ScoreDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Sử dụng IGNORE để bỏ qua nếu tổ hợp điểm bị trùng
    suspend fun insertScore(score: ScoreRecord)

    @Query("SELECT * FROM high_scores ORDER BY date ASC")
    suspend fun getAllScores(): List<ScoreRecord>

    suspend fun getTopScoresForWave(wave: Int): List<ScoreRecord> {
        val scoreColumn = "wave_${wave}_score"

        val query = SimpleSQLiteQuery(
            "SELECT * FROM high_scores WHERE $scoreColumn > 0 ORDER BY $scoreColumn DESC LIMIT 100"
        )

        return getCustomScores(query)
    }

    @RawQuery
    suspend fun getCustomScores(query: androidx.sqlite.db.SupportSQLiteQuery): List<ScoreRecord>
}

// === Database Abstraction ===
@Database(entities = [ScoreRecord::class], version = 2, exportSchema = false) // <--- TĂNG VERSION LÊN 2
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arkanoid_high_scores_db"
                )
                    .fallbackToDestructiveMigration() // <--- GIẢI QUYẾT LỖI APP INSPECTION/SCHEMA
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}