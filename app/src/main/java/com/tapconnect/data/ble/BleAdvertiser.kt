package com.tapconnect.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.*

class BleAdvertiser(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleAdvertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("BleAdvertiser", "Advertising Started Successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BleAdvertiser", "Advertising Failed: $errorCode")
        }
    }

    companion object {
        val TAPCONNECT_SERVICE_UUID = ParcelUuid(UUID.fromString("00007a7c-0000-1000-8000-00805f9b34fb"))
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(userId: String) {
        if (bleAdvertiser == null) {
            Log.e("BleAdvertiser", "Cannot start advertising: BLE advertiser is null")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true) // Universally supported by all BLE chips
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            // We omit the redundant addServiceUuid call to save 18 bytes.
            // This prevents legacy 31-byte overflow errors (ADVERTISE_FAILED_DATA_TOO_LARGE).

        try {
            val uuid = UUID.fromString(userId)
            val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            data.addServiceData(TAPCONNECT_SERVICE_UUID, bb.array())
        } catch (e: Exception) {
            data.addServiceData(TAPCONNECT_SERVICE_UUID, userId.take(16).toByteArray(Charsets.UTF_8))
        }

        try {
            bleAdvertiser.startAdvertising(settings, data.build(), advertiseCallback)
            Log.d("BleAdvertiser", "Advertising started successfully for user: $userId")
        } catch (e: Exception) {
            Log.e("BleAdvertiser", "Failed to start BLE advertising", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        bleAdvertiser?.stopAdvertising(advertiseCallback)
        Log.d("BleAdvertiser", "Advertising Stopped")
    }
}
