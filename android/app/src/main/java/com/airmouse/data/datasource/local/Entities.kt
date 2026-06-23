package com.airmouse.data.datasource.local

import com.airmouse.data.datasource.local.entity.CalibrationEntity
import com.airmouse.data.datasource.local.entity.DailyStatsEntity
import com.airmouse.data.datasource.local.entity.GestureStatsEntity
import com.airmouse.data.datasource.local.entity.GestureTemplateEntity
import com.airmouse.data.datasource.local.entity.ProfileEntity
import com.airmouse.data.datasource.local.entity.SettingsEntity
import com.airmouse.data.datasource.local.entity.StatisticsEntity
import com.airmouse.data.datasource.local.entity.TrainingSampleEntity

data class LocalEntityCatalog(
    val calibration: CalibrationEntity? = null,
    val settings: SettingsEntity? = null,
    val statistics: StatisticsEntity? = null,
    val gestureTemplate: GestureTemplateEntity? = null,
    val gestureStats: GestureStatsEntity? = null,
    val profile: ProfileEntity? = null,
    val dailyStats: DailyStatsEntity? = null,
    val trainingSample: TrainingSampleEntity? = null
)
