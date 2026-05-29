// file: calibration/CalibrationPagerAdapter.kt
package com.airmouse.calibration

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CalibrationPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    companion object {
        const val STEP_COUNT = 3
    }

    override fun getItemCount() = STEP_COUNT

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> GyroComposeFragment()
        1 -> AccelComposeFragment()
        2 -> MagComposeFragment()
        else -> throw IllegalArgumentException()
    }
}