package com.airmouse.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.airmouse.R

class CalibrationFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var timerText: TextView
    private lateinit var backBtn: Button
    private lateinit var nextBtn: Button
    private lateinit var stopBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_calibration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.viewPager)
        timerText = view.findViewById(R.id.timerText)
        backBtn = view.findViewById(R.id.backBtn)
        nextBtn = view.findViewById(R.id.nextBtn)
        stopBtn = view.findViewById(R.id.stopBtn)

        timerText.text = "00:00"

        backBtn.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        nextBtn.setOnClickListener {
            viewPager.currentItem += 1
        }

        stopBtn.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}
