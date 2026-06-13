package com.airmouse.calibration

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CalibrationPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = STEP_COUNT

    override fun createFragment(position: Int): Fragment = Fragment()

    companion object {
        const val STEP_COUNT = 3
    }
}
