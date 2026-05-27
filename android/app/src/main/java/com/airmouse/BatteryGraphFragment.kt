package com.airmouse.ui.battery

import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airmouse.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BatteryGraphFragment : Fragment() {

    private lateinit var batteryChart: LineChart
    private lateinit var batteryTempText: TextView
    private lateinit var batteryLevelText: TextView

    private val temperatureHistory = mutableListOf<Entry>()
    private var index = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_battery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        batteryChart = view.findViewById(R.id.battery_chart)
        batteryTempText = view.findViewById(R.id.battery_temp_text)
        batteryLevelText = view.findViewById(R.id.battery_level_text)

        setupChart()
        startMonitoring()
    }

    private fun setupChart() {
        batteryChart.description.isEnabled = false
        batteryChart.legend.isEnabled = true
        batteryChart.setTouchEnabled(true)
        batteryChart.setPinchZoom(true)
    }

    private fun startMonitoring() {
        lifecycleScope.launch {
            while (true) {
                updateBatteryData()
                delay(5000)
            }
        }
    }

    private fun updateBatteryData() {
        val batteryManager = requireContext().getSystemService(BatteryManager::class.java)
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val temperature = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE) / 10f

        batteryLevelText.text = "Battery Level: $level%"
        batteryTempText.text = "Temperature: ${temperature}°C"

        temperatureHistory.add(Entry(index.toFloat(), temperature))
        index++
        if (temperatureHistory.size > 20) temperatureHistory.removeAt(0)

        val dataSet = LineDataSet(temperatureHistory, "Temperature (°C)")
        dataSet.color = android.graphics.Color.parseColor("#FF5722")
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = 2f
        dataSet.valueTextSize = 10f
        val lineData = LineData(dataSet)
        batteryChart.data = lineData
        batteryChart.invalidate()
    }
}