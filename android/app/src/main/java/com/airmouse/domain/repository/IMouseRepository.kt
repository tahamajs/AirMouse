// app/src/main/java/com/airmouse/domain/repository/IMouseRepository.kt
package com.airmouse.domain.repository

interface IMouseRepository {
    suspend fun sendMove(dx: Float, dy: Float)
    suspend fun sendClick()
    suspend fun sendDoubleClick()
    suspend fun sendRightClick()
    suspend fun sendScroll(delta: Int)
}