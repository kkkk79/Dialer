package com.simpledialer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.CallLog
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

data class CallLogEntry(
    val name: String,
    val number: String,
    val type: Int,
    val date: Long,
    val duration: Long
)

class CallLogActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_log)

        recyclerView = findViewById(R.id.recyclerCallLog)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val required = arrayOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            loadAndExport()
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 2001)
        }
    }

    private fun loadAndExport() {
        val entries = readCallLog()
        showInList(entries)
        exportToCSV(entries)
    }

    private fun readCallLog(): List<CallLogEntry> {
        val entries = mutableListOf<CallLogEntry>()
        val projection = arrayOf(
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        val cursor: Cursor? = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null, null,
            CallLog.Calls.DATE + " DESC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numIdx  = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val durIdx  = it.getColumnIndex(CallLog.Calls.DURATION)
            while (it.moveToNext()) {
                entries.add(CallLogEntry(
                    name     = it.getString(nameIdx) ?: "",
                    number   = it.getString(numIdx)  ?: "",
                    type     = it.getInt(typeIdx),
                    date     = it.getLong(dateIdx),
                    duration = it.getLong(durIdx)
                ))
            }
        }
        return entries
    }

    private fun exportToCSV(entries: List<CallLogEntry>) {
        try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SimpleDialer"
            )
            dir.mkdirs()

            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "call_log_${sdf.format(Date())}.csv"
            val file = File(dir, fileName)

            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            FileWriter(file).use { writer ->
                writer.write("Name,Number,Type,Date,Duration(sec)\n")
                entries.forEach { entry ->
                    val typeStr = when (entry.type) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE   -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Rejected"
                        else                        -> "Unknown"
                    }
                    val name   = entry.name.replace(",", " ")
                    val number = entry.number.replace(",", "")
                    val date   = dateFmt.format(Date(entry.date + 40_000L))
                    writer.write("$name,$number,$typeStr,$date,${entry.duration}\n")
                }
            }

            Toast.makeText(
                this,
                "Saved: Documents/SimpleDialer/$fileName",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInList(entries: List<CallLogEntry>) {
        val adapter = CallLogAdapter(entries) { entry ->
            val intent = Intent(this, MainActivity::class.java).apply {
                data = Uri.parse("tel:${entry.number}")
            }
            startActivity(intent)
            finish()
        }
        recyclerView.adapter = adapter
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001 &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadAndExport()
        } else {
            Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show()
        }
    }
}

class CallLogAdapter(
    private val entries: List<CallLogEntry>,
    private val onClick: (CallLogEntry) -> Unit
) : RecyclerView.Adapter<CallLogAdapter.ViewHolder>() {

    private val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName:     TextView  = view.findViewById(R.id.tvCallName)
        val tvNumber:   TextView  = view.findViewById(R.id.tvCallNumber)
        val tvDate:     TextView  = view.findViewById(R.id.tvCallDate)
        val tvDuration: TextView  = view.findViewById(R.id.tvCallDuration)
        val ivType:     ImageView = view.findViewById(R.id.ivCallType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.tvName.text     = entry.name.ifEmpty { entry.number }
        holder.tvNumber.text   = entry.number
        holder.tvDate.text     = sdf.format(Date(entry.date))
        holder.tvDuration.text = formatDuration(entry.duration)
        val icon = when (entry.type) {
            CallLog.Calls.INCOMING_TYPE -> android.R.drawable.sym_call_incoming
            CallLog.Calls.OUTGOING_TYPE -> android.R.drawable.sym_call_outgoing
            CallLog.Calls.MISSED_TYPE   -> android.R.drawable.sym_call_missed
            else                        -> android.R.drawable.sym_call_incoming
        }
        holder.ivType.setImageResource(icon)
        holder.itemView.setOnClickListener { onClick(entry) }
    }

    override fun getItemCount() = entries.size

    private fun formatDuration(seconds: Long): String {
        val displayed = seconds + 40L
        val m = displayed / 60
        val s = displayed % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
