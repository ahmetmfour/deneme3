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
                rssi >= -50 -> "Çok güçlü"
                rssi >= -60 -> "Güçlü"
                rssi >= -70 -> "Orta"
                rssi >= -80 -> "Zayıf"
                else -> "Çok zayıf"
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
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )

            return
        }

        startLocationUpdates()
        startBleScan()
    }

    // ---------------------------------------------------------
    // KONUM TAKİBİ
    // ---------------------------------------------------------

    private fun startLocationUpdates() {

        val fineGranted =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            return
        }

        try {

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                1f,
                locationListener
            )

            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                2000L,
                1f,
                locationListener
            )

        } catch (e: Exception) {

            locationText.text =
                "📍 Konum alınamadı\n\n${e.message}"
        }
    }

    // ---------------------------------------------------------
    // BLE TARAMASINI BAŞLAT
    // ---------------------------------------------------------

    private fun startBleScan() {

        if (bluetoothAdapter == null) {

            statusText.text =
                "🔴 Bluetooth desteklenmiyor"

            return
        }

        if (!bluetoothAdapter!!.isEnabled) {

            statusText.text =
                "🔵 Lütfen Bluetooth'u aç"

            return
        }

        if (isScanning) {
            return
        }

        val bluetoothScanGranted =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {

                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED

            } else {

                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }

        if (!bluetoothScanGranted) {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    BLUETOOTH_PERMISSION_REQUEST
                )
            }

            return
        }

        try {

            bluetoothScanner =
                bluetoothAdapter!!.bluetoothLeScanner

            if (bluetoothScanner == null) {

                statusText.text =
                    "🔴 BLE Scanner kullanılamıyor"

                return
            }

            deviceFound = false
            isScanning = true

            statusText.text =
                "🔎 BLE cihazları aranıyor..."

            signalText.text =
                "📶 RSSI bekleniyor..."

            scanButton.text =
                "TARAMA DEVAM EDİYOR..."

            bluetoothScanner!!.startScan(
                scanCallback
            )

            handler.removeCallbacks(
                stopScanRunnable
            )

            handler.postDelayed(
                stopScanRunnable,
                SCAN_DURATION
            )

        } catch (e: SecurityException) {

            statusText.text =
                "🔴 Bluetooth izni yok"

            isScanning = false

            scanButton.text =
                "TARAMAYI BAŞLAT"
        }
    }

    // ---------------------------------------------------------
    // BLE TARAMASINI DURDUR
    // ---------------------------------------------------------

    private fun stopScan() {

        if (!isScanning) {
            return
        }

        try {

            bluetoothScanner?.stopScan(
                scanCallback
            )

        } catch (e: SecurityException) {

            // Bluetooth izni yoksa işlem yapılmaz
        }

        isScanning = false

        runOnUiThread {

            if (deviceFound) {

                statusText.text =
                    statusText.text.toString() +
                    "\n\n✅ Tarama tamamlandı"

            } else {

                statusText.text =
                    "🔎 Tarama tamamlandı\n\n" +
                    "BLE cihazı bulunamadı"
            }

            scanButton.text =
                "TARAMAYI BAŞLAT"
        }
    }

    // ---------------------------------------------------------
    // İZİN SONUÇLARI
    // ---------------------------------------------------------

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

        when (requestCode) {

            LOCATION_PERMISSION_REQUEST -> {

                var granted = false

                for (result in grantResults) {

                    if (result == PackageManager.PERMISSION_GRANTED) {
                        granted = true
                    }
                }

                if (granted) {

                    startLocationUpdates()

                    startBleScan()

                } else {

                    locationText.text =
                        "📍 Konum izni verilmedi"

                    statusText.text =
                        "⚠️ Konum izni gerekli"
                }
            }

            BLUETOOTH_PERMISSION_REQUEST -> {

                var granted = false

                for (result in grantResults) {

                    if (result == PackageManager.PERMISSION_GRANTED) {
                        granted = true
                    }
                }

                if (granted) {

                    startBleScan()

                } else {

                    statusText.text =
                        "⚠️ Bluetooth izni verilmedi"

                    scanButton.text =
                        "TARAMAYI BAŞLAT"
                }
            }
        }
    }

    // ---------------------------------------------------------
    // ACTIVITY DURDURULDUĞUNDA
    // ---------------------------------------------------------

    override fun onDestroy() {

        super.onDestroy()

        handler.removeCallbacks(
            stopScanRunnable
        )

        try {

            if (isScanning) {

                bluetoothScanner?.stopScan(
                    scanCallback
                )
            }

        } catch (e: SecurityException) {
            // İzin yoksa işlem yapılmaz
        }

        isScanning = false

        try {

            locationManager.removeUpdates(
                locationListener
            )

        } catch (e: SecurityException) {
            // Konum izni yoksa işlem yapılmaz
        }
    }

    companion object {

        private const val LOCATION_PERMISSION_REQUEST = 1001

        private const val BLUETOOTH_PERMISSION_REQUEST = 1002

        private const val SCAN_DURATION = 10000L
    }
}
