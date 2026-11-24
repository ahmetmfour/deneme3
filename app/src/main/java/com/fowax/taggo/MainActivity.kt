
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
    private var targetFound = false

    private lateinit var locationManager: LocationManager

    private val handler = Handler(Looper.getMainLooper())

    private val targetName = "Nutule_FMN"
    private val targetAddress = "C0:B2:B9:CE:63:D0"

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

            val name = try {

                device.name
                    ?: result.scanRecord?.deviceName

            } catch (e: SecurityException) {

                null
            }

            val address = device.address

            // TAG GO+ kontrolü
            if (
                address.equals(
                    targetAddress,
                    ignoreCase = true
                ) ||
                name == targetName
            ) {

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

                    targetFound = true

                    statusText.text =
                        "🟢 TAG GO+ BULUNDU\n\n" +
                        "Ad: ${name ?: targetName}\n" +
                        "MAC: $address"

                    signalText.text =
                        "📡 RSSI: $rssi dBm\n" +
                        "📶 Sinyal: $signal"
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {

            runOnUiThread {

                statusText.text =
                    "🔴 BLE taraması başarısız\n\n" +
                    "Hata kodu: $errorCode"

                signalText.text =
                    "📡 RSSI: -"

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
                101
            )

            return
        }

        startLocationUpdates()

        requestBluetoothPermissions()
    }

    // ---------------------------------------------------------
    // BLUETOOTH İZNİ
    // ---------------------------------------------------------

    private fun requestBluetoothPermissions() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.S
        ) {

            val scanGranted =
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED

            val connectGranted =
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

            if (!scanGranted || !connectGranted) {

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

        // GPS
        if (requestCode == 101) {

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

            if (fineGranted || coarseGranted) {

                startLocationUpdates()

            } else {

                locationText.text =
                    "📍 GPS izni verilmedi."
            }

            requestBluetoothPermissions()
        }

        // Bluetooth
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
                    "🔵 Bluetooth izinleri verilmedi."
            }
        }
    }

    // ---------------------------------------------------------
    // GPS BAŞLAT
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

            // Son bilinen konum
            val lastLocation =

                if (fineGranted) {

                    locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )

                } else {

                    locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )
                }

            if (lastLocation != null) {

                locationListener.onLocationChanged(
                    lastLocation
                )
            }

            // GPS
            if (
                fineGranted &&
                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            ) {

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    2f,
                    locationListener,
                    Looper.getMainLooper()
                )
            }

            // Network
            if (
                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )
            ) {

                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    5f,
                    locationListener,
                    Looper.getMainLooper()
                )
            }

        } catch (e: SecurityException) {

            locationText.text =
                "📍 GPS erişim izni alınamadı."

        } catch (e: Exception) {

            locationText.text =
                "📍 Konum alınamadı: ${e.message}"
        }
    }

    // ---------------------------------------------------------
    // BLE TARAMA BAŞLAT
    // ---------------------------------------------------------

    private fun startScan() {

        val adapter =
            bluetoothAdapter

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

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            statusText.text =
                "Bluetooth tarama izni gerekli."

            return
        }

        bluetoothScanner =
            adapter.bluetoothLeScanner

        if (bluetoothScanner == null) {

            statusText.text =
                "BLE tarayıcı başlatılamadı."

            return
        }

        targetFound = false

        statusText.text =
            "🔍 Tag Go+ aranıyor..."

        signalText.text =
            "📡 BLE taraması başlatıldı..."

        scanButton.text =
            "TARAMAYI DURDUR"

        isScanning = true

        try {

            bluetoothScanner?.startScan(
                scanCallback
            )

            handler.postDelayed(
                stopScanRunnable,
                15000
            )

        } catch (e: SecurityException) {

            isScanning = false

            scanButton.text =
                "TARAMAYI BAŞLAT"

            statusText.text =
                "Bluetooth tarama izni gerekli."
        }
    }

    // ---------------------------------------------------------
    // BLE TARAMA DURDUR
    // ---------------------------------------------------------

    private fun stopScan() {

        if (!isScanning) {
            return
        }

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }

        try {

            bluetoothScanner?.stopScan(
                scanCallback
            )

        } catch (e: SecurityException) {

            // İzin kaldırılmış olabilir.
        }

        handler.removeCallbacks(
            stopScanRunnable
        )

        isScanning = false

        scanButton.text =
            "TARAMAYI BAŞLAT"

        if (!targetFound) {

            statusText.text =
                "🔴 Tag Go+ bulunamadı."

            signalText.text =
                "📡 RSSI: -"
        }
    }

    // ---------------------------------------------------------
    // ACTIVITY KAPANIRKEN
    // ---------------------------------------------------------

    override fun onDestroy() {

        handler.removeCallbacks(
            stopScanRunnable
        )

        if (isScanning) {

            stopScan()
        }

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            locationManager.removeUpdates(
                locationListener
            )
        }

        super.onDestroy()
    }
}
