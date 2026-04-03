package com.donglm.autonexttool

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var tvCountFloating: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var swipeRunnable: Runnable? = null

    private fun startSwiping() {
        val interval = getValue() * 1000L
        swipeRunnable = object : Runnable {
            override fun run() {
                MyAccessibilityService.instance?.swipeUp()
                handler.postDelayed(this, interval)
            }
        }
        handler.post(swipeRunnable!!)
    }

    private fun stopSwiping() {
        swipeRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        val rootLayout = floatingView.findViewById<View>(R.id.root_floating_layout)
        val layoutExpanded = floatingView.findViewById<View>(R.id.layout_expanded)
        val layoutMinimized = floatingView.findViewById<View>(R.id.layout_minimized)
        
        val btnMinus = floatingView.findViewById<ImageView>(R.id.btnMinusFloating)
        val btnPlus = floatingView.findViewById<ImageView>(R.id.btnPlusFloating)
        val btnStart = floatingView.findViewById<View>(R.id.btnStartFloating)
        val btnSettings = floatingView.findViewById<View>(R.id.btnSettingsFloating)
        val btnMinimize = floatingView.findViewById<View>(R.id.btnMinimizeFloating)
        val ivHandPointer = floatingView.findViewById<View>(R.id.ivHandPointer)
        val btnCloseMinimized = floatingView.findViewById<View>(R.id.btnCloseMinimized)
        tvCountFloating = floatingView.findViewById(R.id.tvCountFloating)

        updateCounter()

        btnMinus.setOnClickListener {
            var timerValue = getValue()
            if (timerValue > 5) {
                timerValue--
                saveValue(timerValue)
                updateCounter()
            }
        }

        btnPlus.setOnClickListener {
            var timerValue = getValue()
            if (timerValue < 60) {
                timerValue++
                saveValue(timerValue)
                updateCounter()
            }
        }

        val rotateAnim = android.animation.ObjectAnimator.ofFloat(ivHandPointer, "rotation", 0f, 360f).apply {
            duration = 1500
            repeatCount = android.animation.ObjectAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
        }

        btnStart.setOnClickListener {
            layoutExpanded.visibility = View.GONE
            layoutMinimized.visibility = View.VISIBLE
            rotateAnim.start()
            startSwiping()
            Toast.makeText(this@FloatingWidgetService, "Auto Swipe Started", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this@FloatingWidgetService, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            stopSelf()
        }

        btnMinimize.setOnClickListener {
            layoutExpanded.visibility = View.GONE
            layoutMinimized.visibility = View.VISIBLE
        }

        ivHandPointer.setOnClickListener {
            layoutExpanded.visibility = View.VISIBLE
            layoutMinimized.visibility = View.GONE
            rotateAnim.cancel()
            ivHandPointer.rotation = 0f
            stopSwiping()
        }

        btnCloseMinimized.setOnClickListener {
            stopSwiping()
            stopSelf()
        }

        // Dragging logic
        val dragTouchListener = object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0.toFloat()
            private var initialTouchY: Float = 0.toFloat()

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val Xdiff = Math.abs((event.rawX - initialTouchX).toInt())
                        val Ydiff = Math.abs((event.rawY - initialTouchY).toInt())
                        if (Xdiff < 10 && Ydiff < 10) {
                            v.performClick()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        }
        
        rootLayout.setOnTouchListener(dragTouchListener)
        ivHandPointer.setOnTouchListener(dragTouchListener)
    }

    private fun updateCounter() {
        tvCountFloating.text = getValue().toString()
    }

    private fun saveValue(value: Int) {
        val sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt("my_number", value)
        editor.apply()
    }

    private fun getValue(): Int {
        val sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        return sharedPref.getInt("my_number", 15) // Default to 15 matching MainActivity
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSwiping()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
