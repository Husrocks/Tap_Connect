package com.tapconnect.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class BleScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private val _discoveredUsers = MutableStateFlow<Set<String>>(emptySet())
    val discoveredUsers: StateFlow<Set<String>> = _discoveredUsers

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceId = extractUserId(result)
            if (deviceId != null) {
                val current = _discoveredUsers.value.toMutableSet()
                if (current.add(deviceId)) {
                    _discoveredUsers.value = current
                    Log.d("BleScanner", "Discovered User: $deviceId")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleScanner", "Scan Failed: $errorCode")
        }
    }

    companion object {
        val TAPCONNECT_SERVICE_UUID = ParcelUuid(UUID.fromString("00007a7c-0000-1000-8000-00805f9b34fb"))
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (bleScanner == null) {
            Log.e("BleScanner", "Cannot start scan: Bluetooth is OFF or not supported")
            return
        }

        // Using null for filters to scan all devices and filter manually in onScanResult
        // This is more reliable for custom Service Data on some Android versions.
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bleScanner.startScan(null, settings, scanCallback)
            Log.d("BleScanner", "Scan Started (Unfiltered)")
        } catch (e: Exception) {
            Log.e("BleScanner", "Failed to start BLE scanning", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            bleScanner?.stopScan(scanCallback)
            Log.d("BleScanner", "Scan Stopped Safely")
        } catch (e: Exception) {
            Log.e("BleScanner", "Failed to stop BLE scanning safely", e)
        } finally {
            _discoveredUsers.value = emptySet()
        }
    }

    private fun extractUserId(result: ScanResult): String? {
        val record = result.scanRecord ?: return null
        
        // Some devices don't parse Service Data into the map correctly.
        // We'll try to find it in the raw bytes if the map is empty.
        val serviceDataMap = record.serviceData
        var targetData: ByteArray? = null
        
        if (serviceDataMap != null) {
            for ((uuid, data) in serviceDataMap) {
                val uuidStr = uuid.uuid.toString().uppercase()
                if (uuidStr.contains("7A7C")) {
                    targetData = data
                    break
                }
            }
        }

        val data = targetData ?: return null
        if (data.size == 16) {
            try {
                val bb = java.nio.ByteBuffer.wrap(data)
                val uuid = java.util.UUID(bb.long, bb.long)
                return uuid.toString()
            } catch (e: Exception) {
                Log.e("BleScanner", "Error parsing UUID bytes", e)
            }
        }
        return null
    }
}
