package com.airmouse.data.datasource.remote

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.ParcelFileDescriptor
import com.airmouse.utils.LogManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.json.JSONObject
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IUsbDataSource {

    companion object {
        private const val TAG = "UsbDataSource"
        private const val ACTION_USB_PERMISSION = "com.airmouse.USB_PERMISSION"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val messageChannel = Channel<ByteArray>(Channel.BUFFERED)
    private val _connectionState = MutableStateFlow(false)

    private var accessory: UsbAccessory? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: BufferedReader? = null
    private var outputStream: PrintWriter? = null
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val acc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY, UsbAccessory::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                    }
                    if (granted && acc != null) {
                        openAccessory(acc)
                    } else {
                        LogManager.warn("USB permission denied", TAG)
                    }
                }
                UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                    disconnect()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
    }

    override fun connect() {
        val accessories = usbManager.accessoryList
        if (accessories.isNullOrEmpty()) {
            LogManager.warn("No USB accessories found", TAG)
            _connectionState.value = false
            return
        }
        val acc = accessories[0]
        if (usbManager.hasPermission(acc)) {
            openAccessory(acc)
        } else {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pi = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
            usbManager.requestPermission(acc, pi)
        }
    }

    private fun openAccessory(acc: UsbAccessory) {
        try {
            fileDescriptor = usbManager.openAccessory(acc)
            if (fileDescriptor == null) {
                LogManager.error("Failed to open USB accessory", TAG)
                _connectionState.value = false
                return
            }
            val fd = fileDescriptor!!.fileDescriptor
            inputStream = BufferedReader(InputStreamReader(FileInputStream(fd)))
            outputStream = PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(fd))), true)
            accessory = acc
            _connectionState.value = true
            LogManager.info("USB accessory connected: ${acc.manufacturer} ${acc.model}", TAG)

            // Send hello
            val hello = JSONObject().apply {
                put("type", "hello")
                put("payload", JSONObject().apply {
                    put("name", "AirMouse")
                    put("version", "2.0")
                    put("device_name", Build.DEVICE)
                    put("model", Build.MODEL)
                    put("manufacturer", Build.MANUFACTURER)
                    put("brand", Build.BRAND)
                    put("android_version", Build.VERSION.RELEASE)
                    put("sdk_int", Build.VERSION.SDK_INT.toString())
                    put("protocol", "usb_serial")
                    put("transport", "usb_accessory")
                })
            }
            send((hello.toString() + "\n").toByteArray(Charsets.UTF_8))

            // Start reading
            readJob = scope.launch {
                try {
                    while (isActive && _connectionState.value) {
                        val line = inputStream?.readLine() ?: break
                        if (line.isNotBlank()) {
                            messageChannel.trySend(line.toByteArray(Charsets.UTF_8))
                        }
                    }
                } catch (e: IOException) {
                    LogManager.error("USB read error", e, TAG)
                } finally {
                    disconnect()
                }
            }
        } catch (e: Exception) {
            LogManager.error("Failed to open USB accessory", e, TAG)
            _connectionState.value = false
        }
    }

    override fun disconnect() {
        readJob?.cancel()
        readJob = null
        try { inputStream?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        try { fileDescriptor?.close() } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        fileDescriptor = null
        accessory = null
        _connectionState.value = false
        LogManager.info("USB accessory disconnected", TAG)
    }

    override fun send(data: ByteArray) {
        try {
            outputStream?.print(String(data, Charsets.UTF_8))
            if (!String(data, Charsets.UTF_8).endsWith("\n")) {
                outputStream?.print("\n")
            }
            outputStream?.flush()
        } catch (e: Exception) {
            LogManager.error("USB write error", e, TAG)
            disconnect()
        }
    }

    override fun observeMessages(): Flow<ByteArray> = messageChannel.receiveAsFlow()
    override fun connectionStatus(): Flow<Boolean> = _connectionState.asStateFlow()

    override fun isConnected(): Boolean = _connectionState.value

    fun cleanup() {
        try { context.unregisterReceiver(usbReceiver) } catch (_: Exception) {}
        disconnect()
        scope.cancel()
    }
}