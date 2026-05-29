package com.airmouse.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airmouse.R
import com.airmouse.utils.LogManager

class ServerLogFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_server_log, container, false)
        recyclerView = view.findViewById(R.id.log_recycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = LogAdapter()
        recyclerView.adapter = adapter

        LogManager.logEntries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
        }
        return view
    }
}

class LogAdapter : RecyclerView.Adapter<LogAdapter.ViewHolder>() {
    private var entries: List<LogManager.LogEntry> = emptyList()

    fun submitList(list: List<LogManager.LogEntry>) {
        entries = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.text1.text = "${entry.timestamp} [${entry.level}]"
        holder.text2.text = entry.message
    }

    override fun getItemCount() = entries.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text1: android.widget.TextView = itemView.findViewById(android.R.id.text1)
        val text2: android.widget.TextView = itemView.findViewById(android.R.id.text2)
    }
}