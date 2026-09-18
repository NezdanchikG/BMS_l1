package com.example.lab1_new

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private var wifiList: List<ScanResult> = ArrayList()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            wifiList = wifiManager.scanResults
            updateWifiList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        listView = findViewById(R.id.listView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter

        val scanButton: Button = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            // Проверяем и запрашиваем разрешения во время выполнения
            checkAndRequestPermissions()

            // Запускаем сканирование только если разрешения уже есть
            if (hasPermissions()) {
                scanWifi()
            }
        }

        // Обработчик клика на элемент списка
        listView.setOnItemClickListener { _, view, position, _ ->
            val selectedSSID = adapter.getItem(position)
            showNetworkDetails(selectedSSID)
        }
        registerReceiver(broadcastReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    private fun checkAndRequestPermissions() {
        val permissionAccessFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val permissionAccessWifiState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_WIFI_STATE
        )

        val listPermissionsNeeded = mutableListOf<String>()

        if (permissionAccessFineLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionAccessWifiState != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE)
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                REQUEST_ID_MULTIPLE_PERMISSIONS
            )
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_WIFI_STATE
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun scanWifi() {
        wifiManager.startScan()
    }

    private fun updateWifiList() {
        adapter.clear()
        for (result in wifiList) {
            adapter.add(result.SSID)
        }
    }

    private fun showNetworkDetails(selectedSSID: String?) {
        selectedSSID?.let {
            val selectedNetwork = wifiList.find { it.SSID == selectedSSID }
            selectedNetwork?.let {
                val levelDescription = calculateSignalLevel(it.level)
                val details = """
                SSID: ${it.SSID}
                BSSID: ${it.BSSID}
                Signal Level: ${it.level} dBm ($levelDescription)
                Frequency: ${it.frequency} MHz
                Capabilities: ${it.capabilities}
            """.trimIndent()

                // Создание диалога
                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setTitle("Network Details")
                dialogBuilder.setMessage(details)
                dialogBuilder.setPositiveButton("OK") { dialog: DialogInterface, _ -> dialog.dismiss() }
                dialogBuilder.create().show()
            }
        }
    }


    private fun calculateSignalLevel(level: Int) = when {
        level > -50 -> "Excellent"
        level in -60..-50 -> "Good"
        level in -70..-60 -> "Fair"
        level < -70 -> "Weak"
        else -> "No signal"
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    companion object {
        const val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
    }
}
