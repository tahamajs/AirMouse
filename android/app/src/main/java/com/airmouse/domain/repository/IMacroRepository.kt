package com.airmouse.domain.repository

import com.airmouse.domain.model.Macro
import com.airmouse.domain.model.MacroAction
import com.airmouse.domain.model.MacroExecutionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for macro recording and playback.
 * Optional – not required for core Air Mouse functionality.
 */
interface IMacroRepository {

    /**
     * Save a macro.
     */
    suspend fun saveMacro(macro: Macro): String

    /**
     * Get a macro by ID.
     */
    suspend fun getMacro(id: String): Macro?

    /**
     * Get all macros.
     */
    suspend fun getAllMacros(): List<Macro>

    /**
     * Delete a macro by ID.
     */
    suspend fun deleteMacro(id: String)

    /**
     * Execute a macro.
     */
    suspend fun executeMacro(id: String): Boolean

    /**
     * Export a macro as JSON.
     */
    suspend fun exportMacro(id: String): String

    /**
     * Import a macro from JSON.
     */
    suspend fun importMacro(data: String): Macro?

    /**
     * Get the total number of macros.
     */
    suspend fun getMacroCount(): Int

    /**
     * Get macros by tag.
     */
    suspend fun getMacrosByTag(tag: String): List<Macro>

    /**
     * Get macros sorted by last used.
     */
    suspend fun getRecentMacros(limit: Int): List<Macro>

    /**
     * Get macros by category.
     */
    suspend fun getMacrosByCategory(category: String): List<Macro>

    /**
     * Toggle macro enabled/disabled.
     */
    suspend fun toggleMacro(id: String): Boolean

    /**
     * Duplicate a macro.
     */
    suspend fun duplicateMacro(id: String): Macro

    /**
     * Clear all macros.
     */
    suspend fun clearAllMacros(): Boolean

    /**
     * Observe macros as a Flow.
     */
    fun observeMacros(): Flow<List<Macro>>

    /**
     * Observe execution status of a macro.
     */
    fun observeExecutionStatus(id: String): Flow<MacroExecutionStatus>
}
