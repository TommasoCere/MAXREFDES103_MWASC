package com.maximintegrated.maximsensorsapp

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import com.maximintegrated.bluetooth.ble.BleScannerActivity
import timber.log.Timber

class ScannerActivity : BleScannerActivity() {

    override fun onBluetoothDeviceClicked(bluetoothDevice: BluetoothDevice) {
        Timber.d("Bluetooth device is clicked")
        MainActivity.start(this, bluetoothDevice)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}