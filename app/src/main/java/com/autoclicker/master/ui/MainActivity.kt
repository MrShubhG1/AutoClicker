package com.autoclicker.master.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autoclicker.master.R
import com.autoclicker.master.accessibility.AutoAccessibilityService
import com.autoclicker.master.overlay.OverlayController
import com.autoclicker.master.services.AutoForegroundService

class MainActivity : AppCompatActivity() {

    private lateinit var overlayController: OverlayController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayController = OverlayController(applicationContext)

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Overlay canvas layer permissions ready", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.btnStartSingle).setOnClickListener {
            launchAutomationOverlayOverlayPanel()
        }

        requestNotificationPermission()
    }

    private fun launchAutomationOverlayOverlayPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please authorize draw window floating permissions layer", Toast.LENGTH_SHORT).show()
            return
        }
        if (AutoAccessibilityService.getInstance() == null) {
            Toast.makeText(this, "Activate accessibility controls service engine in settings menu options first", Toast.LENGTH_LONG).show()
            return
        }

        val serviceIntent = Intent(this, AutoForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        overlayController.showMenu()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 201)
        }
    }
}