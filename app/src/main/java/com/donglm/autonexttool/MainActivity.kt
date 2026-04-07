package com.donglm.autonexttool

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Spinner
import android.view.View
import android.widget.AdapterView
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.AppCompatButton


class MainActivity : ComponentActivity() {
    private val TAG = "donglm"
    private lateinit var txttvPerm1: TextView
    private lateinit var txttvPerm2: TextView

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val tvCount = findViewById<TextView>(R.id.tvCount)
        val btnMinus = findViewById<ImageView>(R.id.btnMinus)
        val btnPlus = findViewById<ImageView>(R.id.btnPlus)
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val spinnerDirection = findViewById<Spinner>(R.id.spinnerDirection)
        val imvSetting1 = findViewById<ImageView>(R.id.ivSettings1)
        val imvSetting2 = findViewById<ImageView>(R.id.ivSettings2)
        txttvPerm1 = findViewById<TextView>(R.id.tvPerm1)
        txttvPerm2 = findViewById<TextView>(R.id.tvPerm2)
        val bntstart = findViewById<AppCompatButton>(R.id.btnStart)
        check()
        findViewById<AppCompatButton>(R.id.btnStart).setOnClickListener {
            val hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
            val hasAccessibility = isAccessibilityServiceEnabled(this, MyAccessibilityService::class.java)

            if (!hasOverlay) {
                requestOverlayPermission()
            } else if (!hasAccessibility) {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Grant Accessibility Permission")
                    .setMessage("The App needs Accessibility Permission to automatically swipe the screen. Please click 'Open Settings' and enable AutoNextTool.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        openAccessibilitySettings()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                startService(Intent(this@MainActivity, FloatingWidgetService::class.java))
                moveTaskToBack(true)
            }
        }

        var timerValue = getValue()

        fun updateUI() {
            tvCount.text = timerValue.toString()
            seekBar.progress = timerValue - 1
            tvDescription.text = "Automatically swipe the screen every $timerValue seconds."
        }
        imvSetting1.setOnClickListener({
            requestOverlayPermission()
        })
        imvSetting2.setOnClickListener({
            if (!isAccessibilityServiceEnabled(this, MainActivity::class.java)) {
                openAccessibilitySettings()
            }
        })

        btnMinus.setOnClickListener {
            if (timerValue > 5) {
                timerValue--
                saveValue(timerValue)
                updateUI()
            }
        }

        btnPlus.setOnClickListener {
            if (timerValue < 60) {
                timerValue++
                saveValue(timerValue)
                updateUI()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    timerValue = progress + 1
                    updateUI()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val sharedPref = getSharedPreferences("MyApp", MODE_PRIVATE)
        val savedDirection = sharedPref.getInt("swipe_direction", 0)
        spinnerDirection.setSelection(savedDirection)

        spinnerDirection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val editor = sharedPref.edit()
                editor.putInt("swipe_direction", position)
                editor.apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        updateUI()
    }
    override fun onResume() {
        super.onResume()
        check( )
    }
    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)

        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${service.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(expectedComponentName)
    }


    fun check() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {

                txttvPerm1.setTextColor(Color.GREEN)
            } else {

                txttvPerm1.setTextColor(Color.RED)
            }
        } else {
            // Android < 6 defaults to having permission
            txttvPerm1.setTextColor(Color.GREEN)
        }

        if (!isAccessibilityServiceEnabled(this, MyAccessibilityService::class.java)){
            Log.d(TAG, "isAccessibilityServiceEnabled: = false")
            txttvPerm2.setTextColor(Color.RED);
        }
        else {
            Log.d(TAG, "isAccessibilityServiceEnabled: = true")
            txttvPerm2.setTextColor(Color.GREEN);
        }
    }
    fun saveValue(value: Int) {
            val sharedPref = getSharedPreferences("MyApp", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt("my_number", value)
        editor.apply()
    }

    fun getValue(): Int {
        val sharedPref = getSharedPreferences("MyApp", MODE_PRIVATE)
        return sharedPref.getInt("my_number", 15) // 15 is default
    }
}