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

    private val stopScanRunnable = Runnable {
        stopBleScan()
    }

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            runOnUiThread {
                val device = result.device
                val name = try {
                    device.name ?: "İsimsiz cihaz"
                } catch (e: SecurityException) {
                    "İsim alınamadı"
                }

                val rssi = result.rssi

                statusText.text =
                    "Cihaz bulundu:\n\n" +
                    "Ad: $name\n" +
                    "Adres: ${device.address}\n" +
                    "RSSI: $rssi dBm"
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                statusText.text =
                    "BLE taraması başarısız.\nHata kodu: $errorCode"

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
                "Bu telefonda Bluetooth bulunamadı."
            return
        }

        if (!adapter.isEnabled) {
            statusText.text =
                "Bluetooth kapalı. Önce Bluetooth'u aç."
            return
        }

        if (isScanning) {
            stopBleScan()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {

            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        bluetoothScanner = adapter.bluetoothLeScanner

        if (bluetoothScanner == null) {
            statusText.text =
                "BLE tarayıcı başlatılamadı."
            return
        }

        statusText.text =
            "BLE cihazları aranıyor..."

        isScanning = true
        scanButton.text = "TARAMAYI DURDUR"

        bluetoothScanner?.startScan(scanCallback)

        handler.postDelayed(
            stopScanRunnable,
            10000
        )
    }

    private fun stopBleScan() {

        if (!isScanning) {
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {

            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        bluetoothScanner?.stopScan(scanCallback)

        isScanning = false

        scanButton.text = "TARAMAYI BAŞLAT"

        statusText.text =
            "Tarama tamamlandı."
    }

    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacks(stopScanRunnable)

        if (isScanning) {
            stopBleScan()
        }
    }
}
