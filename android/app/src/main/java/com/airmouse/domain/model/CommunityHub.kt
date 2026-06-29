package com.airmouse.domain.model

data class CommunityHub(
    val isEnabled: Boolean = false,
    val userId: String = "",
    val deviceName: String = "",
    val sharedProfiles: List<String> = emptyList(),
    val communityGestures: List<String> = emptyList(),
    val lastSync: Long = System.currentTimeMillis()
)