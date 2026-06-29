package com.airmouse.domain.repository

import com.airmouse.domain.model.CommunityHub

/**
 * Repository for community features.
 * Optional – not required for core Air Mouse functionality.
 */
interface ICommunityRepository {

    /**
     * Fetch trending community items.
     */
    suspend fun fetchTrending(): List<CommunityItem>

    /**
     * Fetch community gesture templates.
     */
    suspend fun fetchCommunityGestures(): List<CommunityGesture>

    /**
     * Download a community item by ID.
     */
    suspend fun downloadItem(id: String): Boolean

    /**
     * Share a community item.
     */
    suspend fun shareItem(id: String): Boolean

    /**
     * Get the current community hub state.
     */
    suspend fun getCommunityHub(): CommunityHub

    /**
     * Update the community hub state.
     */
    suspend fun updateCommunityHub(hub: CommunityHub)

    /**
     * Search community content.
     */
    suspend fun searchCommunity(query: String): List<CommunityItem>

    /**
     * Rate a community item.
     */
    suspend fun rateItem(id: String, rating: Int): Boolean

    /**
     * Get community notifications.
     */
    suspend fun getNotifications(): List<CommunityNotification>

    /**
     * Mark notification as read.
     */
    suspend fun markNotificationRead(notificationId: String): Boolean
}

data class CommunityItem(
    val id: String,
    val title: String,
    val description: String,
    val author: String,
    val rating: Float,
    val downloads: Int,
    val category: String,
    val createdAt: Long
)

data class CommunityGesture(
    val id: String,
    val name: String,
    val action: String,
    val confidence: Float,
    val downloads: Int,
    val rating: Float,
    val author: String
)

data class CommunityNotification(
    val id: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: String = "info"
)