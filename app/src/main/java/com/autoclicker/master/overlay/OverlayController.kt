package com.autoclicker.master.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.autoclicker.master.R
import com.autoclicker.master.automation.AutomationEngine
import com.autoclicker.master.storage.ClickPoint
import com.autoclicker.master.storage.ProfileManager

class OverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val profileManager = ProfileManager(context)
    private var menuView: View? = null
    
    private val targetPoints = mutableListOf<View>()
    private val pointDataList = mutableListOf<ClickPoint>()
    private var isPlaying = false

    @SuppressLint("ClickableViewAccessibility")
    fun showMenu() {
        if (menuView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        menuView = LayoutInflater.from(context).inflate(R.layout.overlay_menu, null)
        
        val dragBtn = menuView!!.findViewById<ImageView>(R.id.btn_drag)
        val playBtn = menuView!!.findViewById<ImageView>(R.id.btn_play)
        val addBtn = menuView!!.findViewById<ImageView>(R.id.btn_add)
        val saveBtn = menuView!!.findViewById<ImageView>(R.id.btn_save_profile)
        val clearBtn = menuView!!.findViewById<ImageView>(R.id.btn_clear)
        val closeBtn = menuView!!.findViewById<ImageView>(R.id.btn_close)

        dragBtn.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(menuView, params)
                        return true
                    }
                }
                return false
            }
        })

        addBtn.setOnClickListener {
            addNewClickTarget()
        }

        clearBtn.setOnClickListener {
            clearTargets()
        }

        saveBtn.setOnClickListener {
            syncCoordinatesData()
            profileManager.saveProfile("default_profile", pointDataList)
            Toast.makeText(context, "Automation target configuration profile stored!", Toast.LENGTH_SHORT).show()
        }

        playBtn.setOnClickListener {
            if (!isPlaying) {
                if (targetPoints.isEmpty()) {
                    Toast.makeText(context, "Add targets before starting automation loop", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                isPlaying = true
                playBtn.setImageResource(android.R.drawable.ic_media_pause)
                syncCoordinatesData()
                AutomationEngine.instance.startAutomation(pointDataList)
            } else {
                isPlaying = false
                playBtn.setImageResource(android.R.drawable.ic_media_play)
                AutomationEngine.instance.stopAutomation()
            }
        }

        closeBtn.setOnClickListener {
            clearTargets()
            hideMenu()
        }

        windowManager.addView(menuView, params)
        
        val savedPoints = profileManager.loadProfile("default_profile")
        if (savedPoints.isNotEmpty()) {
            for (savedPoint in savedPoints) {
                reconstructSavedTarget(savedPoint)
            }
        }
    }

    private fun hideMenu() {
        menuView?.let {
            windowManager.removeView(it)
            menuView = null
        }
        AutomationEngine.instance.stopAutomation()
    }

    private fun addNewClickTarget() {
        val newId = targetPoints.size + 1
        val dummyPoint = ClickPoint(newId, 300 + (newId * 25), 600, 250L, 50L)
        reconstructSavedTarget(dummyPoint)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun reconstructSavedTarget(pointConfig: ClickPoint) {
        val pointView = LayoutInflater.from(context).inflate(R.layout.overlay_point, null)
        val numText = pointView.findViewById<TextView>(R.id.point_number)
        numText.text = pointConfig.id.toString()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = pointConfig.x
            y = pointConfig.y
        }

        pointView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var lastActionDown = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastActionDown = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(pointView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (System.currentTimeMillis() - lastActionDown < 200) {
                            showIndividualPointSettings(pointConfig, pointView)
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(pointView, params)
        targetPoints.add(pointView)
        pointDataList.add(pointConfig)
    }

    private fun showIndividualPointSettings(pointConfig: ClickPoint, parentView: View) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_point_settings, null)
        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val etDelay = dialogView.findViewById<EditText>(R.id.et_delay)
        val etDuration = dialogView.findViewById<EditText>(R.id.et_duration)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        title.text = "Target Configuration #" + pointConfig.id
        etDelay.setText(pointConfig.delayMs.toString())
        etDuration.setText(pointConfig.durationMs.toString())

        val dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        btnCancel.setOnClickListener { windowManager.removeView(dialogView) }
        btnSave.setOnClickListener {
            try {
                pointConfig.delayMs = etDelay.text.toString().toLong()
                pointConfig.durationMs = etDuration.text.toString().toLong()
                Toast.makeText(context, "Metrics updated for target " + pointConfig.id, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Malformed metric number error input layout value", Toast.LENGTH_SHORT).show()
            }
            windowManager.removeView(dialogView)
        }

        windowManager.addView(dialogView, dialogParams)
    }

    private fun syncCoordinatesData() {
        for (i in targetPoints.indices) {
            val view = targetPoints[i]
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            val data = pointDataList[i]
            data.x = layoutParams.x + (view.width / 2)
            data.y = layoutParams.y + (view.height / 2)
        }
    }

    private fun clearTargets() {
        for (view in targetPoints) {
            windowManager.removeView(view)
        }
        targetPoints.clear()
        pointDataList.clear()
        isPlaying = false
        menuView?.findViewById<ImageView>(R.id.btn_play)?.setImageResource(android.R.drawable.ic_media_play)
        AutomationEngine.instance.stopAutomation()
    }
}