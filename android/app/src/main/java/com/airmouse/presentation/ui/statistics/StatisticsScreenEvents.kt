
package com.airmouse.presentation.ui.statistics

sealed class StatisticsScreenEvent {
    
    object LoadData : StatisticsScreenEvent()
    object RefreshData : StatisticsScreenEvent()

    
    data class SelectTimeRange(val timeRange: TimeRange) : StatisticsScreenEvent()

    
    data class SelectChart(val chartType: ChartType) : StatisticsScreenEvent()

    
    object ShowExportDialog : StatisticsScreenEvent()
    object DismissExportDialog : StatisticsScreenEvent()
    data class ExportData(val format: ExportFormat) : StatisticsScreenEvent()

    
    object ShowResetDialog : StatisticsScreenEvent()
    object DismissResetDialog : StatisticsScreenEvent()
    object ConfirmReset : StatisticsScreenEvent()

    
    data class DismissError(val error: String) : StatisticsScreenEvent()
    data class DismissSuccess(val message: String) : StatisticsScreenEvent()
}