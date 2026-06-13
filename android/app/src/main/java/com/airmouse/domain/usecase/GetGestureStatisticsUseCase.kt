// app/src/main/java/com/airmouse/domain/usecase/GetGestureStatisticsUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.repository.IGestureRepository
import javax.inject.Inject

class GetGestureStatisticsUseCase @Inject constructor(
    private val gestureRepo: IGestureRepository
) {
    suspend operator fun invoke() = gestureRepo.getGestureStats()
}

package com.airmouse.domain.usecase

import com.airmouse.domain.model.GestureStatistics
import com.airmouse.domain.repository.IGestureRepository
import javax.inject.Inject

class GetGestureStatisticsUseCase @Inject constructor(
    private val repo: IGestureRepository
) {
    suspend operator fun invoke(): GestureStatistics = repo.getGestureStats()
}