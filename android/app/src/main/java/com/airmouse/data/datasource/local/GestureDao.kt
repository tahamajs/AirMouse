// app/src/main/java/com/airmouse/data/datasource/local/GestureDao.kt
package com.airmouse.data.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureDao {

    // ==================== Insert/Update ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: GestureTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplateIfNotExists(template: GestureTemplateEntity)

    @Update
    suspend fun updateTemplate(template: GestureTemplateEntity)

    // ==================== Get All ====================

    @Query("SELECT * FROM gesture_templates ORDER BY name ASC")
    suspend fun getAllTemplates(): List<GestureTemplateEntity>

    @Query("SELECT * FROM gesture_templates ORDER BY name ASC")
    fun observeAllTemplates(): Flow<List<GestureTemplateEntity>>

    @Query("SELECT * FROM gesture_templates ORDER BY detection_count DESC")
    suspend fun getAllTemplatesByUsage(): List<GestureTemplateEntity>

    // ==================== Get By ID ====================

    @Query("SELECT * FROM gesture_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): GestureTemplateEntity?

    @Query("SELECT * FROM gesture_templates WHERE id = :id")
    fun observeTemplateById(id: String): Flow<GestureTemplateEntity?>

    // ==================== Get By Name ====================

    @Query("SELECT * FROM gesture_templates WHERE name = :name")
    suspend fun getTemplateByName(name: String): GestureTemplateEntity?

    @Query("SELECT * FROM gesture_templates WHERE name LIKE '%' || :query || '%'")
    suspend fun searchTemplates(query: String): List<GestureTemplateEntity>

    @Query("SELECT * FROM gesture_templates WHERE name LIKE '%' || :query || '%'")
    fun observeSearchTemplates(query: String): Flow<List<GestureTemplateEntity>>

    // ==================== Get By Type ====================

    @Query("SELECT * FROM gesture_templates WHERE type = :type")
    suspend fun getTemplatesByType(type: String): List<GestureTemplateEntity>

    @Query("SELECT * FROM gesture_templates WHERE type = :type")
    fun observeTemplatesByType(type: String): Flow<List<GestureTemplateEntity>>

    // ==================== Get By Status ====================

    @Query("SELECT * FROM gesture_templates WHERE is_enabled = 1")
    suspend fun getEnabledTemplates(): List<GestureTemplateEntity>

    @Query("SELECT * FROM gesture_templates WHERE is_enabled = 1")
    fun observeEnabledTemplates(): Flow<List<GestureTemplateEntity>>

    @Query("SELECT * FROM gesture_templates WHERE is_enabled = 0")
    suspend fun getDisabledTemplates(): List<GestureTemplateEntity>

    // ==================== Get Custom/System ====================

    @Query("SELECT * FROM gesture_templates WHERE is_custom = 1")
    suspend fun getCustomTemplates(): List<GestureTemplateEntity>

    @Query("SELECT * FROM gesture_templates WHERE is_custom = 1")
    fun observeCustomTemplates(): Flow<List<GestureTemplateEntity>>

    @Query("SELECT * FROM gesture_templates WHERE is_system = 1")
    suspend fun getSystemTemplates(): List<GestureTemplateEntity>

    @Query("SELECT * FROM gesture_templates WHERE is_system = 1")
    fun observeSystemTemplates(): Flow<List<GestureTemplateEntity>>

    // ==================== Get Favorites ====================

    @Query("SELECT * FROM gesture_templates WHERE is_favorite = 1")
    suspend fun getFavoriteTemplates(): List<GestureTemplateEntity>

    @Query("SELECT * FROM gesture_templates WHERE is_favorite = 1")
    fun observeFavoriteTemplates(): Flow<List<GestureTemplateEntity>>

    // ==================== Update Operations ====================

    @Query("UPDATE gesture_templates SET detection_count = detection_count + 1, last_detected = :timestamp WHERE id = :id")
    suspend fun incrementDetectionCount(id: String, timestamp: Long)

    @Query("UPDATE gesture_templates SET is_enabled = :enabled WHERE id = :id")
    suspend fun setTemplateEnabled(id: String, enabled: Boolean)

    @Query("UPDATE gesture_templates SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setTemplateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE gesture_templates SET confidence_threshold = :threshold WHERE id = :id")
    suspend fun updateConfidenceThreshold(id: String, threshold: Float)

    @Query("UPDATE gesture_templates SET training_samples_count = training_samples_count + 1 WHERE id = :id")
    suspend fun incrementTrainingSamples(id: String)

    @Query("UPDATE gesture_templates SET version = version + 1, updated_at = :timestamp WHERE id = :id")
    suspend fun incrementVersion(id: String, timestamp: Long)

    @Query("UPDATE gesture_templates SET metadata = :metadata WHERE id = :id")
    suspend fun updateMetadata(id: String, metadata: String)

    // ==================== Delete Operations ====================

    @Query("DELETE FROM gesture_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Query("DELETE FROM gesture_templates")
    suspend fun deleteAllTemplates()

    @Query("DELETE FROM gesture_templates WHERE is_custom = 1 AND is_system = 0")
    suspend fun deleteAllCustomTemplates()

    // ==================== Count Operations ====================

    @Query("SELECT COUNT(*) FROM gesture_templates")
    suspend fun getTemplateCount(): Int

    @Query("SELECT COUNT(*) FROM gesture_templates WHERE is_custom = 1")
    suspend fun getCustomTemplateCount(): Int

    @Query("SELECT COUNT(*) FROM gesture_templates WHERE is_enabled = 1")
    suspend fun getEnabledTemplateCount(): Int

    @Query("SELECT COUNT(*) FROM gesture_templates WHERE is_favorite = 1")
    suspend fun getFavoriteTemplateCount(): Int

    // ==================== Type Distribution ====================

    @Query("SELECT type, COUNT(*) as count FROM gesture_templates GROUP BY type")
    suspend fun getTypeDistribution(): List<GestureTypeCount>

    // ==================== Statistics ====================

    @Query("SELECT SUM(detection_count) FROM gesture_templates")
    suspend fun getTotalDetections(): Int

    @Query("SELECT AVG(confidence_threshold) FROM gesture_templates")
    suspend fun getAverageConfidenceThreshold(): Float

    @Query("SELECT MAX(detection_count) FROM gesture_templates")
    suspend fun getMaxDetectionCount(): Int

    // ==================== Bulk Operations ====================

    @Query("SELECT * FROM gesture_templates WHERE id IN (:ids)")
    suspend fun getTemplatesByIds(ids: List<String>): List<GestureTemplateEntity>

    @Query("SELECT * FROM gesture_templates ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getRecentlyUpdated(limit: Int): List<GestureTemplateEntity>
}
