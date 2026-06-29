package com.airmouse.data.repository

import com.airmouse.domain.model.CommunityHub as DomainCommunityHub
import com.airmouse.domain.repository.CommunityGesture
import com.airmouse.domain.repository.CommunityItem
import com.airmouse.domain.repository.CommunityNotification
import com.airmouse.domain.repository.ICommunityRepository
import com.airmouse.utils.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ICommunityRepository {

    override suspend fun fetchTrending(): List<CommunityItem> {
        return emptyList()
    }

    override suspend fun fetchCommunityGestures(): List<CommunityGesture> {
        return emptyList()
    }

    override suspend fun downloadItem(id: String): Boolean {
        return true
    }

    override suspend fun shareItem(id: String): Boolean {
        return true
    }

    override suspend fun getCommunityHub(): DomainCommunityHub {
        return DomainCommunityHub()
    }

    override suspend fun updateCommunityHub(hub: DomainCommunityHub) {
        // Mock sync/update logic if needed
    }

    override suspend fun searchCommunity(query: String): List<CommunityItem> {
        return emptyList()
    }

    override suspend fun rateItem(id: String, rating: Int): Boolean {
        return true
    }

    override suspend fun getNotifications(): List<CommunityNotification> {
        return emptyList()
    }

    override suspend fun markNotificationRead(notificationId: String): Boolean {
        return true
    }
}
