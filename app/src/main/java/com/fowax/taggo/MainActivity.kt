package com.fowax.taggo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var statusText: TextView
    private lateinit var locationText: TextView
    private lateinit var signalText: TextView
    private lateinit var scanButton: Button

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothScanner: BluetoothLeScanner? = null

    private var isScanning = false
    private var deviceFound = false

    private lateinit var locationManager: LocationManager

    private val handler = Handler(Looper.getMainLooper())

    private val stopScanRunnable = Runnable {
        stopScan()
    }

    // ---------------------------------------------------------
    // GPS
    // ---------------------------------------------------------

    private val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {

            runOnUiThread {

                locationText.text =
                    "📍 TELEFON KONUMU\n\n" +
                    "Enlem: ${
                        String.format(
                            Locale.US,
                            "%.6f",
                            location.latitude
                        )
                    }\n" +
                    "Boylam: ${
                        String.format(
                            Locale.US,
                            "%.6f",
                            location.longitude
                        )
                    }\n\n" +
                    "Hassasiyet: ${location.accuracy.toInt()} metre"
            }
        }
    }

    // ---------------------------------------------------------
    // BLE TARAMA
    // ---------------------------------------------------------

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {

            val device = result.device

            val address = try {

                device.address

            } catch (e: SecurityException) {

                "Bilinmiyor"
            }

            val name = try {

                device.name
                    ?: result.scanRecord?.deviceName
                    ?: "İsimsiz cihaz"

            } catch (e: SecurityException) {

                "İsimsiz cihaz"
            }

            val rssi = result.rssi

            val signal = when {

                rssi >= -50 ->
                    "Çok güçlü"

                rssi >= -60 ->
                    "Güçlü"

                rssi >= -70 ->
                    "Orta"

                rssi >= -80 ->
                    "Zayıf"

                else ->
                    "Çok zayıf"
            }

            runOnUiThread {

                deviceFound = true

                statusText.text =
                    "📡 BLE CİHAZI BULUNDU\n\n" +
                    "Ad: $name\n" +
                    "MAC: $address"

                signalText.text =
                    "📶 RSSI: $rssi dBm\n" +
                    "Sinyal: $signal"
            }
        }

        override fun onScanFailed(errorCode: Int) {

            runOnUiThread {

                statusText.text =
                    "🔴 BLE TARAMASI BAŞARISIZ\n\n" +
                    "Hata kodu: $errorCode"

                signalText.text =
                    "📶 RSSI: -"

                isScanning = false

                scanButton.text =
                    "TARAMAYI BAŞLAT"
            }
        }
    }

    // ---------------------------------------------------------
    // ACTIVITY
    // ---------------------------------------------------------

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        statusText =
            findViewById(R.id.statusText)

        locationText =
            findViewById(R.id.locationText)

        signalText =
            findViewById(R.id.signalText)

        scanButton =
            findViewById(R.id.scanButton)

        // Bluetooth
        val bluetoothManager =
            getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as BluetoothManager

        bluetoothAdapter =
            bluetoothManager.adapter

        // GPS
        locationManager =
            getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        scanButton.setOnClickListener {

            requestLocationPermission()
        }
    }

    // ---------------------------------------------------------
    // KONUM İZNİ
    // ---------------------------------------------------------

    private fun requestLocationPermission() {

        val fineGranted =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager
