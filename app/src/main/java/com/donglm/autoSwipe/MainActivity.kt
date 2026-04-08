package com.donglm.autoSwipe

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.AppCompatButton
import com.donglm.autoSwipe.R


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
        val cbDoubleClick = findViewById<CheckBox>(R.id.cbDoubleClick)
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

                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.grant_appear_on_top_title))
                    .setMessage(getString(R.string.grant_appear_on_top_msg))
                    .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                        requestOverlayPermission()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()

            } else if (!hasAccessibility) {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.grant_accessibility_title))
                    .setMessage(getString(R.string.grant_accessibility_msg))
                    .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                        openAccessibilitySettings()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
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
            tvDescription.text = getString(R.string.automatically_swipe_the_screen_every_x_seconds, timerValue)
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

        val directions = resources.getStringArray(R.array.swipe_directions)
        val adapter = object : android.widget.ArrayAdapter<String>(this, R.layout.layout_spinner_item, R.id.tvSpinnerText, directions) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val ivIcon = view.findViewById<ImageView>(R.id.ivSpinnerIcon)
                when (position) {
                    0 -> ivIcon.rotation = 0f
                    1 -> ivIcon.rotation = 180f
                    2 -> ivIcon.rotation = 270f
                    3 -> ivIcon.rotation = 90f
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val ivIcon = view.findViewById<ImageView>(R.id.ivSpinnerIcon)
                when (position) {
                    0 -> ivIcon.rotation = 0f
                    1 -> ivIcon.rotation = 180f
                    2 -> ivIcon.rotation = 270f
                    3 -> ivIcon.rotation = 90f
                }
                return view
            }
        }
        spinnerDirection.adapter = adapter
        spinnerDirection.setSelection(savedDirection)
        spinnerDirection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val editor = sharedPref.edit()
                editor.putInt("swipe_direction", position)
                editor.apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        cbDoubleClick.isChecked = sharedPref.getBoolean("enable_double_click", false)
        cbDoubleClick.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPref.edit()
            editor.putBoolean("enable_double_click", isChecked)
            editor.apply()
        }

        updateUI()
    }
    override fun onResume() {
        super.onResume()
        check( )
        val sharedPref = getSharedPreferences("MyApp", MODE_PRIVATE)
        val spinnerDirection = findViewById<android.widget.Spinner>(R.id.spinnerDirection)
        val cbDoubleClick = findViewById<android.widget.CheckBox>(R.id.cbDoubleClick)
        spinnerDirection.setSelection(sharedPref.getInt("swipe_direction", 0))
        cbDoubleClick.isChecked = sharedPref.getBoolean("enable_double_click", false)
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