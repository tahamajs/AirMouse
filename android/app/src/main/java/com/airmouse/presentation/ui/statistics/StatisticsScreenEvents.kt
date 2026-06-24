package com.airmouse.presentation.ui.statistics

/**
 * Events that can be sent to the StatisticsViewModel.
 */
sealed class StatisticsScreenEvent {

    /** Load or refresh data */
    object LoadData : StatisticsScreenEvent()
    object RefreshData : StatisticsScreenEvent()

    /** Select time range for statistics */
    data class SelectTimeRange(val timeRange: TimeRange) : StatisticsScreenEvent()

    /** Select chart type to display */
    data class SelectChart(val chartType: ChartType) : StatisticsScreenEvent()

    /** Export data */
    object ShowExportDialog : StatisticsScreenEvent()
    object DismissExportDialog : StatisticsScreenEvent()
    data class ExportData(val format: ExportFormat) : StatisticsScreenEvent()

    /** Reset statistics */
    object ShowResetDialog : StatisticsScreenEvent()
    object DismissResetDialog : StatisticsScreenEvent()
    object ConfirmReset : StatisticsScreenEvent()

    /** Dismiss error or success messages */
    data class DismissError(val error: String) : StatisticsScreenEvent()
    data class DismissSuccess(val message: String) : StatisticsScreenEvent()
}