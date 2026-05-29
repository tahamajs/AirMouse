// file: calibration/CalibrationPagerAdapter.kt
package com.airmouse.calibration

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CalibrationPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 3
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> GyroStepFragment()
        1 -> AccelStepFragment()
        2 -> MagStepFragment()
        else -> throw IllegalArgumentException()
    }
}