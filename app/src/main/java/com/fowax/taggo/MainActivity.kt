package com.fowax.taggo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {

    private lateinit var statusText: TextView
    private lateinit var scanButton: Button

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var isScanning = false

    private val handler = Handler(Looper.getMainLooper())

    private val targetName = "Nutule_FMN"
    private val targetAddress = "C0:B2:B9:CE:63:D0"

    private val stopScanRunnable = Runnable {
        stopScan()
    }

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            val device = result.device

            val name = try {
                device.name ?: result.scanRecord?.deviceName
            } catch (e: SecurityException) {
                null
            }

            val address = device.address

            if (
                address.equals(targetAddress, ignoreCase = true) ||
                name == targetName
            ) {
                val rssi = result.rssi

                runOnUiThread {
                    statusText.text =
                        "TAG GO+ BULUNDU\n\n" +
                        "Ad: $targetName\n" +
                        "MAC: $address\n\n" +
                        "RSSI: $rssi dBm"
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                statusText.text =
                    "Tarama başarısız.\nHata: $errorCode"

                isScanning = false
                scanButton.text = "TARAMAYI BAŞLAT"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        scanButton = findViewById(R.id.scanButton)

        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothAdapter = bluetoothManager.adapter

        scanButton.setOnClickListener {
            requestBluetoothPermissions()
        }
    }

    private fun requestBluetoothPermissions() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {

            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    100
                )

                return
            }
        }

        startScan()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100) {

            if (
                grantResults.isNotEmpty() &&
                grantResults.all {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                startScan()
            } else {
                statusText.text =
                    "Bluetooth izinleri verilmedi."
            }
        }
    }

    private fun startScan() {

        val adapter = bluetoothAdapter

        if (adapter == null) {
            statusText.text =
                "Bluetooth bulunamadı."
            return
        }

        if (!adapter.isEnabled) {
            statusText.text =
                "Bluetooth kapalı."
            return
        }

        if (isScanning) {
            stopScan()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        bluetoothScanner = adapter.bluetoothLeScanner

        if (bluetoothScanner == null) {
            statusText.text =
                "BLE tarayıcı başlatılamadı."
            return
        }

        statusText.text =
            "Tag Go+ aranıyor..."

        scanButton.text =
            "TARAMAYI DURDUR"

        isScanning = true

        bluetoothScanner?.startScan(scanCallback)

        handler.postDelayed(
            stopScanRunnable,
            15000
        )
    }

    private fun stopScan() {

        if (!isScanning) {
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        bluetoothScanner?.stopScan(scanCallback)

        isScanning = false

        scanButton.text =
            "TARAMAYI BAŞLAT"

        statusText.text =
            "Tarama tamamlandı."
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopScanRunnable)

        if (isScanning) {
            stopScan()
        }

        super.onDestroy()
    }
}
