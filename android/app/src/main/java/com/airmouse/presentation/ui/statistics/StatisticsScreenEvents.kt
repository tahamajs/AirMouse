// app/src/main/java/com/airmouse/presentation/ui/statistics/StatisticsScreenEvents.kt
package com.airmouse.presentation.ui.statistics

/**
 * Events that can occur on the statistics screen
 */
sealed class StatisticsScreenEvent {
    // Data loading
    object LoadData : StatisticsScreenEvent()
    object RefreshData : StatisticsScreenEvent()

    // Time range selection
    data class SelectTimeRange(val timeRange: TimeRange) : StatisticsScreenEvent()

    // Chart selection
    data class SelectChart(val chartType: ChartType) : StatisticsScreenEvent()

    // Export
    object ShowExportDialog : StatisticsScreenEvent()
    object DismissExportDialog : StatisticsScreenEvent()
    data class ExportData(val format: ExportFormat) : StatisticsScreenEvent()

    // Reset
    object ShowResetDialog : StatisticsScreenEvent()
    object DismissResetDialog : StatisticsScreenEvent()
    object ConfirmReset : StatisticsScreenEvent()

    // Error handling
    data class DismissError(val error: String) : StatisticsScreenEvent()
    data class DismissSuccess(val message: String) : StatisticsScreenEvent()
}