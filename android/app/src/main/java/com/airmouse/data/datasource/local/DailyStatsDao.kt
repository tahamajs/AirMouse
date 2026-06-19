// app/src/main/java/com/airmouse/data/datasource/local/DailyStatsDao.kt
package com.airmouse.data.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {

    // ==================== Insert/Update ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStats(stats: DailyStatsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyStatsIfNotExists(stats: DailyStatsEntity)

    // ==================== Get By Date ====================

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getDailyStats(date: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    fun observeDailyStats(date: String): Flow<DailyStatsEntity?>

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getDailyStatsOrEmpty(date: String): DailyStatsEntity {
        return getDailyStats(date) ?: DailyStatsEntity(date = date)
    }

    // ==================== Get Range ====================

    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    suspend fun getDailyStatsRange(startDate: String, endDate: String): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    fun observeDailyStatsRange(startDate: String, endDate: String): Flow<List<DailyStatsEntity>>

    // ==================== Get Recent ====================

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDailyStats(limit: Int): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :limit")
    fun observeRecentDailyStats(limit: Int): Flow<List<DailyStatsEntity>>

    // ==================== Get By Date Range with Aggregation ====================

    @Query("""
        SELECT 
            SUM(clicks) as totalClicks,
            SUM(double_clicks) as totalDoubleClicks,
            SUM(right_clicks) as totalRightClicks,
            SUM(scrolls) as totalScrolls,
            SUM(movements) as totalMovements,
            SUM(distance) as totalDistance,
            SUM(gestures) as totalGestures
        FROM daily_stats 
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getAggregatedStats(startDate: String, endDate: String): AggregatedStats?

    // ==================== Increment Operations ====================

    @Query("UPDATE daily_stats SET clicks = clicks + :count WHERE date = :date")
    suspend fun incrementClicks(date: String, count: Int = 1)

    @Query("UPDATE daily_stats SET double_clicks = double_clicks + :count WHERE date = :date")
    suspend fun incrementDoubleClicks(date: String, count: Int = 1)

    @Query("UPDATE daily_stats SET right_clicks = right_clicks + :count WHERE date = :date")
    suspend fun incrementRightClicks(date: String, count: Int = 1)

    @Query("UPDATE daily_stats SET scrolls = scrolls + :count WHERE date = :date")
    suspend fun incrementScrolls(date: String, count: Int = 1)

    @Query("UPDATE daily_stats SET movements = movements + :count, distance = distance + :distance WHERE date = :date")
    suspend fun incrementMovements(date: String, count: Int = 1, distance: Float = 0f)

    @Query("UPDATE daily_stats SET gestures = gestures + :count WHERE date = :date")
    suspend fun incrementGestures(date: String, count: Int = 1)

    // ==================== Reset Operations ====================

    @Query("UPDATE daily_stats SET clicks = 0, double_clicks = 0, right_clicks = 0, scrolls = 0, movements = 0, distance = 0, gestures = 0 WHERE date = :date")
    suspend fun resetDailyStats(date: String)

    @Query("UPDATE daily_stats SET clicks = :clicks, double_clicks = :doubleClicks, right_clicks = :rightClicks, scrolls = :scrolls, movements = :movements, distance = :distance, gestures = :gestures WHERE date = :date")
    suspend fun updateDailyStats(
        date: String,
        clicks: Int,
        doubleClicks: Int,
        rightClicks: Int,
        scrolls: Int,
        movements: Int,
        distance: Float,
        gestures: Int
    )

    // ==================== Delete Operations ====================

    @Query("DELETE FROM daily_stats WHERE date < :date")
    suspend fun deleteStatsOlderThan(date: String)

    @Query("DELETE FROM daily_stats WHERE date BETWEEN :startDate AND :endDate")
    suspend fun deleteStatsRange(startDate: String, endDate: String)

    @Query("DELETE FROM daily_stats")
    suspend fun deleteAllStats()

    @Query("DELETE FROM daily_stats WHERE date = :date")
    suspend fun deleteDailyStats(date: String)

    // ==================== Count Operations ====================

    @Query("SELECT COUNT(*) FROM daily_stats")
    suspend fun getDaysWithStats(): Int

    @Query("SELECT COUNT(*) FROM daily_stats WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getDaysWithStatsInRange(startDate: String, endDate: String): Int

    // ==================== Analytics Operations ====================

    @Query("SELECT MAX(clicks) FROM daily_stats")
    suspend fun getMaxClicksInDay(): Int

    @Query("SELECT MAX(movements) FROM daily_stats")
    suspend fun getMaxMovementsInDay(): Int

    @Query("SELECT MAX(distance) FROM daily_stats")
    suspend fun getMaxDistanceInDay(): Float

    @Query("SELECT date FROM daily_stats ORDER BY clicks DESC LIMIT 1")
    suspend fun getMostActiveDay(): String?

    @Query("SELECT date FROM daily_stats ORDER BY distance DESC LIMIT 1")
    suspend fun getMostDistanceDay(): String?

    @Query("""
        SELECT 
            AVG(clicks) as avgClicks,
            AVG(movements) as avgMovements,
            AVG(distance) as avgDistance
        FROM daily_stats 
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getAverageStats(startDate: String, endDate: String): AverageStats?

    // ==================== Check Operations ====================

    @Query("SELECT EXISTS(SELECT 1 FROM daily_stats WHERE date = :date)")
    suspend fun exists(date: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM daily_stats WHERE date = :date AND (clicks > 0 OR movements > 0))")
    suspend fun hasActivity(date: String): Boolean

    // ==================== Bulk Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStatsList(stats: List<DailyStatsEntity>)

    @Query("SELECT * FROM daily_stats WHERE date IN (:dates)")
    suspend fun getDailyStatsForDates(dates: List<String>): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats WHERE date >= :startDate ORDER BY date")
    suspend fun getDailyStatsFromDate(startDate: String): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats WHERE date <= :endDate ORDER BY date DESC")
    suspend fun getDailyStatsUpToDate(endDate: String): List<DailyStatsEntity>
}

// ==================== Data Classes for Aggregated Results ====================

data class AggregatedStats(
    val totalClicks: Int,
    val totalDoubleClicks: Int,
    val totalRightClicks: Int,
    val totalScrolls: Int,
    val totalMovements: Int,
    val totalDistance: Float,
    val totalGestures: Int
)

data class AverageStats(
    val avgClicks: Float,
    val avgMovements: Float,
    val avgDistance: Float
)