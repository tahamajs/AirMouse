package com.airmouse.ui.statistics   // adjust package to your actual package

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airmouse.R
import com.airmouse.utils.PreferencesManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment() {

    private lateinit var preferences: PreferencesManager
    private lateinit var gestureCountText: TextView
    private lateinit var sessionTimeText: TextView
    private lateinit var barChart: BarChart

    private var sessionStartTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())

        gestureCountText = view.findViewById(R.id.gesture_count_text)
        sessionTimeText = view.findViewById(R.id.session_time_text)
        barChart = view.findViewById(R.id.gesture_chart)

        setupChart()
        updateUI()
        startSessionTimer()
    }

    private fun updateUI() {
        val click = preferences.getClickCount()
        val scroll = preferences.getScrollCount()
        val right = preferences.getRightClickCount()
        val double = preferences.getDoubleClickCount()
        gestureCountText.text = getString(R.string.gesture_counts, click, scroll, right, double)
        updateChart(click, scroll, right, double)
    }

    private fun setupChart() {
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.setFitBars(true)
        barChart.axisLeft.setDrawGridLines(false)
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false
    }

    private fun updateChart(click: Int, scroll: Int, right: Int, double: Int) {
        val entries = listOf(
            BarEntry(0f, click.toFloat()),
            BarEntry(1f, scroll.toFloat()),
            BarEntry(2f, right.toFloat()),
            BarEntry(3f, double.toFloat())
        )
        val dataSet = BarDataSet(entries, "Gestures")
        dataSet.setColors(
            android.graphics.Color.parseColor("#FF5722"),
            android.graphics.Color.parseColor("#4CAF50"),
            android.graphics.Color.parseColor("#2196F3"),
            android.graphics.Color.parseColor("#FFC107")
        )
        dataSet.valueTextSize = 12f
        val barData = BarData(dataSet)
        barChart.data = barData
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Click", "Scroll", "Right", "Double"))
        barChart.invalidate()
    }

    private fun startSessionTimer() {
        sessionStartTime = System.currentTimeMillis()
        lifecycleScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - sessionStartTime
                val seconds = (elapsed / 1000) % 60
                val minutes = (elapsed / (1000 * 60)) % 60
                val hours = elapsed / (1000 * 60 * 60)
                sessionTimeText.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                delay(1000)
            }
        }
    }
}