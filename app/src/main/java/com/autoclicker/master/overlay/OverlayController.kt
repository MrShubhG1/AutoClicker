package com.autoclicker.master.overlay

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
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
    private var isTargetsVisible = true // State to track Jadu Mode (Hide/Show)

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
        val toggleVisibilityBtn = menuView!!.findViewById<ImageView>(R.id.btn_toggle_visibility)
        val saveBtn = menuView!!.findViewById<ImageView>(R.id.btn_save_profile)
        val shareBtn = menuView!!.findViewById<ImageView>(R.id.btn_share_profile)
        val importBtn = menuView!!.findViewById<ImageView>(R.id.btn_import_profile)
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

        addBtn.setOnClickListener { addNewClickTarget() }
        clearBtn.setOnClickListener { clearTargets() }

        // Toggling Jadu Mode (Hiding target views while maintaining accessibility engine operations)
        toggleVisibilityBtn.setOnClickListener {
            isTargetsVisible = !isTargetsVisible
            if (isTargetsVisible) {
                toggleVisibilityBtn.setColorFilter(Color.parseColor("#4CAF50")) // Green when visible
                Toast.makeText(context, "Targets visible!", Toast.LENGTH_SHORT).show()
            } else {
                toggleVisibilityBtn.setColorFilter(Color.parseColor("#F44336")) // Red when hidden
                Toast.makeText(context, "Jadu mode active! Targets hidden.", Toast.LENGTH_SHORT).show()
            }
            
            // Loop through existing targets to toggle layout layer invisibility natively
            for (view in targetPoints) {
                view.visibility = if (isTargetsVisible) View.VISIBLE else View.INVISIBLE
            }
        }

        saveBtn.setOnClickListener {
            syncCoordinatesData()
            profileManager.saveProfile("default_profile", pointDataList)
            Toast.makeText(context, "Local configuration profile stored!", Toast.LENGTH_SHORT).show()
        }

        shareBtn.setOnClickListener {
            syncCoordinatesData()
            val exportString = profileManager.getSerializedProfile(pointDataList)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("AutoClickerScript", exportString))
            Toast.makeText(context, "Script string copied to clipboard!", Toast.LENGTH_LONG).show()
        }

        importBtn.setOnClickListener { showImportScriptDialog() }

        playBtn.setOnClickListener {
            if (!isPlaying) {
                if (targetPoints.isEmpty()) {
                    Toast.makeText(context, "Add targets before starting automation", Toast.LENGTH_SHORT).show()
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
        val dummyPoint = ClickPoint(newId, 300 + (newId * 25), 600, 250L, 50L, false)
        reconstructSavedTarget(dummyPoint)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun reconstructSavedTarget(pointConfig: ClickPoint) {
        val pointView = LayoutInflater.from(context).inflate(R.layout.overlay_point, null)
        val numText = pointView.findViewById<TextView>(R.id.point_number)
        val bgView = pointView.findViewById<View>(R.id.point_background)
        
        if (pointConfig.isBack) {
            numText.text = "B"
            bgView.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#9C27B0"))
        } else {
            numText.text = pointConfig.id.toString()
            bgView.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF3333"))
        }

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
                // Ignore touch adjustments if points are programmatically set to invisible layout state
                if (!isTargetsVisible) return false

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

        // Apply visibility state directly if user reconstructs target while hidden
        pointView.visibility = if (isTargetsVisible) View.VISIBLE else View.INVISIBLE

        windowManager.addView(pointView, params)
        targetPoints.add(pointView)
        pointDataList.add(pointConfig)
    }

    private fun showIndividualPointSettings(pointConfig: ClickPoint, parentView: View) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_point_settings, null)
        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val cbIsBack = dialogView.findViewById<CheckBox>(R.id.cb_is_back)
        val etDelay = dialogView.findViewById<EditText>(R.id.et_delay)
        val etDuration = dialogView.findViewById<EditText>(R.id.et_duration)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        title.text = "Target Configuration #" + pointConfig.id
        cbIsBack.isChecked = pointConfig.isBack
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
                pointConfig.isBack = cbIsBack.isChecked
                pointConfig.delayMs = etDelay.text.toString().toLong()
                pointConfig.durationMs = etDuration.text.toString().toLong()
                
                val numText = parentView.findViewById<TextView>(R.id.point_number)
                val bgView = parentView.findViewById<View>(R.id.point_background)
                if (pointConfig.isBack) {
                    numText.text = "B"
                    bgView.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#9C27B0"))
                } else {
                    numText.text = pointConfig.id.toString()
                    bgView.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF3333"))
                }
                
                Toast.makeText(context, "Metrics updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Malformed metric configuration", Toast.LENGTH_SHORT).show()
            }
            windowManager.removeView(dialogView)
        }

        windowManager.addView(dialogView, dialogParams)
    }

    private fun showImportScriptDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_point_settings, null)
        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val cbIsBack = dialogView.findViewById<CheckBox>(R.id.cb_is_back)
        val etInput = dialogView.findViewById<EditText>(R.id.et_delay)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        title.text = "Import Shared Script JSON"
        cbIsBack.visibility = View.GONE
        dialogView.findViewById<View>(R.id.et_duration).visibility = View.GONE
        
        etInput.hint = "Paste serialized script text here"
        etInput.setText("")
        etInput.inputType = android.text.InputType.TYPE_CLASS_TEXT

        val dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        btnCancel.setOnClickListener { windowManager.removeView(dialogView) }
        btnSave.setOnClickListener {
            val text = etInput.text.toString()
            val importedList = profileManager.parseSerializedProfile(text)
            if (importedList.isNotEmpty()) {
                clearTargets()
                for (pt in importedList) {
                    reconstructSavedTarget(pt)
                }
                profileManager.saveProfile("default_profile", pointDataList)
                Toast.makeText(context, "External script imported successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Invalid script format layout token error", Toast.LENGTH_SHORT).show()
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
            data.x = layoutParams.x
            data.y = layoutParams.y
        }
    }

    private fun clearTargets() {
        for (view in targetPoints) {
            windowManager.removeView(view)
        }
        targetPoints.clear()
        pointDataList.clear()
        isPlaying = false
        isTargetsVisible = true
        menuView?.findViewById<ImageView>(R.id.btn_play)?.setImageResource(android.R.drawable.ic_media_play)
        menuView?.findViewById<ImageView>(R.id.btn_toggle_visibility)?.setColorFilter(Color.parseColor("#4CAF50"))
        AutomationEngine.instance.stopAutomation()
    }
}