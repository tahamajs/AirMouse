package com.airmouse.ui.log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.utils.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

class ServerLogFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LogAdapter
    private val logEntries = mutableListOf<String>()
    private lateinit var preferences: PreferencesManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_server_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())
        recyclerView = view.findViewById(R.id.log_recycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = LogAdapter(logEntries)
        recyclerView.adapter = adapter

        // Load persisted logs
        val savedLogs = preferences.getServerLogs()
        logEntries.addAll(savedLogs)
        adapter.notifyItemRangeInserted(0, savedLogs.size)

        // Listen for new logs via callback
        (requireActivity().application as? MyApplication)?.logListener = object : LogListener {
            override fun onNewLog(message: String) {
                requireActivity().runOnUiThread {
                    logEntries.add(0, message)
                    adapter.notifyItemInserted(0)
                    recyclerView.scrollToPosition(0)
                }
            }
        }
    }

    interface LogListener {
        fun onNewLog(message: String)
    }

    inner class LogAdapter(private val entries: List<String>) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {
        inner class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(parent.context)
            tv.setPadding(16, 12, 16, 12)
            tv.textSize = 12f
            tv.typeface = android.graphics.Typeface.MONOSPACE
            return ViewHolder(tv)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = entries[position]
        }
        override fun getItemCount() = entries.size
    }
}