// app/src/main/java/com/airmouse/data/repository/MouseRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.repository.IMouseRepository
import javax.inject.Inject

class MouseRepositoryImpl @Inject constructor(
    private val connectionRepo: IConnectionRepository
) : IMouseRepository {

    override suspend fun sendMove(dx: Float, dy: Float) {
        connectionRepo.sendEvent(MouseEvent.Move(dx, dy))
    }

    override suspend fun sendClick() {
        connectionRepo.sendEvent(MouseEvent.Click)
    }

    override suspend fun sendDoubleClick() {
        connectionRepo.sendEvent(MouseEvent.DoubleClick)
    }

    override suspend fun sendRightClick() {
        connectionRepo.sendEvent(MouseEvent.RightClick)
    }

    override suspend fun sendScroll(delta: Int) {
        connectionRepo.sendEvent(MouseEvent.Scroll(delta))
    }
}