package com.donglm.autoSwipe

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.postDelayed
import com.donglm.autoSwipe.R

class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var tvCountFloating: TextView
    private lateinit var layoutExpanded: View
    private lateinit var layoutMinimized: View
    private lateinit var ivHandPointer: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private var swipeRunnable: Runnable? = null
    private var swipeAnim: android.animation.ValueAnimator? = null
    private var screenOffReceiver: android.content.BroadcastReceiver? = null

    private val permissionCheckRunnable = object : Runnable {
        override fun run() {
            val hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(this@FloatingWidgetService)
            val hasAccessibility = MyAccessibilityService.instance != null
            if (!hasOverlay || !hasAccessibility) {
                stopSwipingAndResetUI()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun stopSwipingAndResetUI() {
        if (!::layoutExpanded.isInitialized) return
        if (layoutMinimized.visibility == View.VISIBLE) {
            layoutExpanded.visibility = View.VISIBLE
            layoutMinimized.visibility = View.GONE
            swipeAnim?.cancel()
            ivHandPointer.translationX = 0f
            ivHandPointer.translationY = 0f
            ivHandPointer.rotation = 0f
            ivHandPointer.alpha = 1f
            ivHandPointer.setImageResource(R.drawable.arrow_red)
            ivHandPointer.clearColorFilter()
            stopSwiping()
        }
    }

    private fun startSwiping() {
        val interval = getValue() * 1000L

        swipeRunnable = object : Runnable {
            override fun run() {
                handler.postDelayed(this, interval)
                swipeAnim?.start()
                MyAccessibilityService.instance?.performSwipe()
                val sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
                val isDoubleClickEnabled = sharedPref.getBoolean("enable_double_click", false)
                if (isDoubleClickEnabled) {
                    handler.postDelayed({
                        MyAccessibilityService.instance?.performDoubleClick()
                        
                        // Change icon to double_tap
                        ivHandPointer.setImageResource(R.drawable.double_tap)
                        
                        // Double click animation (scale down and up twice)
                        val clickAnim = android.animation.ObjectAnimator.ofPropertyValuesHolder(
                            ivHandPointer,
                            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.8f, 1f, 0.8f, 1f),
                            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.8f, 1f, 0.8f, 1f)
                        )
                        clickAnim.duration = 500
                        clickAnim.start()
                        
                        // Revert to arrow_green after 0.5s (500ms)
                        handler.postDelayed({
                            if (layoutMinimized.visibility == View.VISIBLE) {
                                ivHandPointer.setImageResource(R.drawable.arrow_green)
                            }
                        }, 500L)
                    }, 2000L)
                }
                

            }
        }
        handler.postDelayed(swipeRunnable!!, 2000)
    }

    private fun stopSwiping() {
        swipeRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        
        screenOffReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    stopSwipingAndResetUI()
                }
            }
        }
        val filter = android.content.IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
        
        handler.post(permissionCheckRunnable)

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
        layoutExpanded = floatingView.findViewById<View>(R.id.layout_expanded)
        layoutMinimized = floatingView.findViewById<View>(R.id.layout_minimized)
        
        val btnMinus = floatingView.findViewById<ImageView>(R.id.btnMinusFloating)
        val btnPlus = floatingView.findViewById<ImageView>(R.id.btnPlusFloating)
        val btnStart = floatingView.findViewById<View>(R.id.btnStartFloating)
        val btnSettings = floatingView.findViewById<View>(R.id.btnSettingsFloating)
        val btnMinimize = floatingView.findViewById<View>(R.id.btnMinimizeFloating)
        val btnChangeDirectionFloating = floatingView.findViewById<View>(R.id.btnChangeDirectionFloating)
        val ivChangeDirectionFloating = floatingView.findViewById<ImageView>(R.id.ivChangeDirectionFloating)
        val btnToggleDoubleClick = floatingView.findViewById<View>(R.id.btnToggleDoubleClick)
        val ivDoubleClickFloating = floatingView.findViewById<ImageView>(R.id.ivDoubleClickFloating)
        val btnCloseExpanded = floatingView.findViewById<View>(R.id.btnCloseExpanded)
        ivHandPointer = floatingView.findViewById<ImageView>(R.id.ivHandPointer)
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

        btnStart.setOnClickListener {
            layoutExpanded.visibility = View.GONE
            layoutMinimized.visibility = View.VISIBLE
            
            val sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
            val direction = sharedPref.getInt("swipe_direction", 0)

            val moveDistance = 60f
            when (direction) {
                0 -> ivHandPointer.rotation = 0f
                1 -> ivHandPointer.rotation = 180f
                2 -> ivHandPointer.rotation = 270f
                3 -> ivHandPointer.rotation = 90f
            }
            ivHandPointer.setImageResource(R.drawable.arrow_green)
            ivHandPointer.clearColorFilter()

            swipeAnim = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1000
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener { animator ->
                    val fraction = animator.animatedFraction
                    val progressDistance = fraction * moveDistance
                    
                    val alpha = when {
                        fraction < 0.2f -> fraction / 0.2f
                        fraction > 0.8f -> (1f - fraction) / 0.2f
                        else -> 1f
                    }
                    ivHandPointer.alpha = alpha

                    when (direction) {
                        0 -> ivHandPointer.translationY = -progressDistance
                        1 -> ivHandPointer.translationY = progressDistance
                        2 -> ivHandPointer.translationX = -progressDistance
                        3 -> ivHandPointer.translationX = progressDistance
                    }
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        super.onAnimationEnd(animation)
                        ivHandPointer.alpha = 1f
                        ivHandPointer.translationX = 0f
                        ivHandPointer.translationY = 0f
                    }
                })
            }
            
            startSwiping()
//            Toast.makeText(this@FloatingWidgetService, getString(R.string.auto_swipe_started), Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this@FloatingWidgetService, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            stopSelf()
        }

        fun updateExtraToggles() {
            val sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
            val dir = sharedPref.getInt("swipe_direction", 0)
            when (dir) {
                0 -> ivChangeDirectionFloating.rotation = 0f
                1 -> ivChangeDirectionFloating.rotation = 180f
                2 -> ivChangeDirectionFloating.rotation = 270f
                3 -> ivChangeDirectionFloating.rotation = 90f
            }
            val isDouble = sharedPref.getBoolean("enable_double_click", false)
            ivDoubleClickFloating.alpha = if (isDouble) 1f else 0.3f
        }
        updateExtraToggles()

        btnChangeDirectionFloating.setOnClickListener {
            val sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
            val dir = sharedPref.getInt("swipe_direction", 0)
            val nextDir = (dir + 1) % 4
            sharedPref.edit().putInt("swipe_direction", nextDir).apply()
            updateExtraToggles()
            val dirStr = when(nextDir){
                0 -> getString(R.string.direction_up)
                1 -> getString(R.string.direction_down)
                2 -> getString(R.string.direction_left)
                else -> getString(R.string.direction_right)
            }
            Toast.makeText(this@FloatingWidgetService, getString(R.string.direction_set, dirStr), Toast.LENGTH_SHORT).show()
        }

        btnToggleDoubleClick.setOnClickListener {
            val sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
            val isDouble = sharedPref.getBoolean("enable_double_click", false)
            sharedPref.edit().putBoolean("enable_double_click", !isDouble).apply()
            updateExtraToggles()
            val msg = if (!isDouble) getString(R.string.double_click_enabled) else getString(R.string.double_click_disabled)
            Toast.makeText(this@FloatingWidgetService, msg, Toast.LENGTH_SHORT).show()
        }

        btnCloseExpanded.setOnClickListener {
            stopSwiping()
            stopSelf()
        }

        btnMinimize.setOnClickListener {
            layoutExpanded.visibility = View.GONE
            layoutMinimized.visibility = View.VISIBLE
            ivHandPointer.alpha = 1f
            val sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
            val direction = sharedPref.getInt("swipe_direction", 0)
            when (direction) {
                0 -> ivHandPointer.rotation = 0f
                1 -> ivHandPointer.rotation = 180f
                2 -> ivHandPointer.rotation = 270f
                3 -> ivHandPointer.rotation = 90f
            }
            ivHandPointer.setImageResource(R.drawable.arrow_red)
            ivHandPointer.clearColorFilter()
        }

        ivHandPointer.setOnClickListener {
            stopSwipingAndResetUI()
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
        handler.removeCallbacks(permissionCheckRunnable)
        screenOffReceiver?.let { unregisterReceiver(it) }
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
