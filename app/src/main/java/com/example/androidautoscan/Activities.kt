package com.example.androidautoscan

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// =====================================================================
// AndroidAutoScan — fixture for accessibility autoScan TRIGGER COMMANDS.
// One page per Appium endpoint. autoScan fires on the command but the scan
// is SKIPPED when the screenshot's pHash is >95% similar to any previous
// scan — so every page devotes its bottom half to a "stage panel" with a
// page-unique vivid colour + giant text BEFORE the gesture, swapping to a
// completely different colour + text AFTER the gesture. Page entry and the
// gesture itself therefore each produce a pHash-distinct screen.
// Each page also keeps a live counter and one unlabeled ImageButton so
// every scan has an identifiable finding.
// =====================================================================

private fun Activity.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

/** Bottom-half stage panel: starts in the "before" look; fire() swaps to "after". */
private class Stage(
    val panel: TextView,
    private val afterColor: Int,
    private val afterText: String,
) {
    fun fire() {
        panel.setBackgroundColor(afterColor)
        panel.text = afterText
    }
}

/**
 * Page skeleton: title + content on top, unlabeled image button (per-page
 * violation), then the stage panel filling the remaining ~50% of the screen.
 */
private fun Activity.page(
    title: String,
    beforeColor: Int, beforeText: String,
    afterColor: Int, afterText: String,
    content: LinearLayout.(Stage) -> Unit,
) {
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        fitsSystemWindows = true
    }
    val top = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(8))
    }
    top.addView(TextView(this).apply {
        text = title
        textSize = 22f
        setPadding(0, 0, 0, dp(10))
    })
    val panel = TextView(this).apply {
        text = beforeText
        textSize = 32f
        setTextColor(0xFFFFFFFF.toInt())
        gravity = Gravity.CENTER
        setBackgroundColor(beforeColor)
        contentDescription = "stage_panel"
    }
    val stage = Stage(panel, afterColor, afterText)
    top.content(stage)
    top.addView(ImageButton(this).apply {
        setImageResource(android.R.drawable.ic_menu_info_details)   // unlabeled: violation
    }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { topMargin = dp(8) })
    root.addView(top, LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
    root.addView(panel, LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.1f))
    setContentView(root)
}

private fun TextView.bump(prefix: String) {
    val n = (text.toString().substringAfter(": ").toIntOrNull() ?: 0) + 1
    text = "$prefix: $n"
}

class HomeActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val scroll = ScrollView(this).apply { fitsSystemWindows = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        root.addView(TextView(this).apply {
            text = "AutoScan Trigger Pages"
            textSize = 22f
            setPadding(0, 0, 0, dp(12))
        })
        val pages: List<Pair<String, Class<out Activity>>> = listOf(
            "Click" to ClickActivity::class.java,
            "Tap" to TapActivity::class.java,
            "Value" to ValueActivity::class.java,
            "Clear" to ClearActivity::class.java,
            "Swipe" to SwipeActivity::class.java,
            "Scroll" to ScrollActivity::class.java,
            "LongPress" to LongPressActivity::class.java,
            "DoubleTap" to DoubleTapActivity::class.java,
            "Flick" to FlickActivity::class.java,
            "Actions" to ActionsActivity::class.java,
            "Back" to BackActivity::class.java,
            "Orientation" to OrientationActivity::class.java,
        )
        for ((label, cls) in pages) {
            root.addView(Button(this).apply {
                text = label
                isAllCaps = false
                contentDescription = "open_${label.lowercase()}"
                setOnClickListener { startActivity(Intent(this@HomeActivity, cls)) }
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) })
        }
        scroll.addView(root)
        setContentView(scroll)
    }
}

class ClickActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Click page",
            0xFF1E88E5.toInt(), "CLICK\nWAITING",
            0xFFFFC107.toInt(), "CLICK\nDONE ✔") { stage ->
            val counter = TextView(context).apply { text = "clicks: 0"; textSize = 16f; contentDescription = "click_count" }
            addView(Button(context).apply {
                text = "Click me"
                isAllCaps = false
                contentDescription = "btn_click"
                setOnClickListener { counter.bump("clicks"); stage.fire() }
            })
            addView(counter)
        }
    }
}

class TapActivity : Activity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Tap page",
            0xFF43A047.toInt(), "TAP\nWAITING",
            0xFFE91E63.toInt(), "TAP\nDONE ✔") { stage ->
            val counter = TextView(context).apply { text = "taps: 0"; textSize = 16f; contentDescription = "tap_count" }
            addView(View(context).apply {
                setBackgroundColor(0xFF90CAF9.toInt())
                contentDescription = "tap_area"
                setOnClickListener { counter.bump("taps"); stage.fire() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpOf(110)))
            addView(counter)
        }
    }
}

class ValueActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Value page",
            0xFF8E24AA.toInt(), "VALUE\nEMPTY",
            0xFF00BCD4.toInt(), "VALUE\nTEXT TYPED ✔") { stage ->
            addView(EditText(context).apply {
                hint = "Type here"
                contentDescription = "input_value"
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (!s.isNullOrEmpty()) stage.fire()
                    }
                    override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                })
            })
            addView(TextView(context).apply { text = "typed text appears above"; textSize = 14f; contentDescription = "value_echo" })
        }
    }
}

class ClearActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Clear page",
            0xFF6D4C41.toInt(), "CLEAR\nHAS TEXT",
            0xFFCDDC39.toInt(), "CLEAR\nEMPTIED ✔") { stage ->
            addView(EditText(context).apply {
                setText("Clear this text")
                contentDescription = "input_clear"
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (s.isNullOrEmpty()) stage.fire()
                    }
                    override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                })
            })
        }
    }
}

abstract class TouchCounterActivity(
    private val title: String,
    private val areaDesc: String,
    private val counterDesc: String,
    private val counterPrefix: String,
    private val areaColor: Int,
    private val beforeColor: Int, private val beforeText: String,
    private val afterColor: Int, private val afterText: String,
) : Activity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page(title, beforeColor, beforeText, afterColor, afterText) { stage ->
            val counter = TextView(context).apply { text = "$counterPrefix: 0"; textSize = 16f; contentDescription = counterDesc }
            addView(View(context).apply {
                setBackgroundColor(areaColor)
                contentDescription = areaDesc
                var downX = 0f; var downY = 0f
                setOnTouchListener { _, e ->
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y; true }
                        MotionEvent.ACTION_UP -> {
                            if (kotlin.math.abs(e.x - downX) > 80 || kotlin.math.abs(e.y - downY) > 80 || countTouchesToo()) {
                                counter.bump(counterPrefix); stage.fire()
                            }
                            true
                        }
                        else -> true
                    }
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpOf(130)))
            addView(counter)
        }
    }
    open fun countTouchesToo() = false
}

class SwipeActivity : TouchCounterActivity("Swipe page", "swipe_area", "swipe_count", "swipes",
    0xFFFFAB91.toInt(), 0xFFF4511E.toInt(), "SWIPE\nWAITING", 0xFF3949AB.toInt(), "SWIPE\nDONE ✔")

class FlickActivity : TouchCounterActivity("Flick page", "flick_area", "flick_count", "flicks",
    0xFF81D4FA.toInt(), 0xFF039BE5.toInt(), "FLICK\nWAITING", 0xFFFB8C00.toInt(), "FLICK\nDONE ✔")

class ActionsActivity : TouchCounterActivity("Actions page", "actions_area", "actions_count", "gestures",
    0xFFE6EE9C.toInt(), 0xFF9E9D24.toInt(), "ACTIONS\nWAITING", 0xFF8D6E63.toInt(), "ACTIONS\nDONE ✔") {
    override fun countTouchesToo() = true
}

class ScrollActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        // The scroll page IS its own stage: vivid full-width colour bands mean any
        // scroll position shows a different band composition to the pHash.
        val scroll = ScrollView(this).apply { fitsSystemWindows = true; contentDescription = "scroll_container" }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        root.addView(TextView(this).apply { text = "Scroll page"; textSize = 22f; setPadding(0, 0, 0, dp(12)) })
        root.addView(ImageButton(this).apply { setImageResource(android.R.drawable.ic_menu_info_details) },
            LinearLayout.LayoutParams(dp(48), dp(48)))
        val band = intArrayOf(0xFFE53935.toInt(), 0xFFFDD835.toInt(), 0xFF43A047.toInt(),
                              0xFF1E88E5.toInt(), 0xFF8E24AA.toInt(), 0xFFFB8C00.toInt())
        repeat(40) { i ->
            root.addView(TextView(this).apply {
                text = "Row ${i + 1}"
                textSize = 20f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(band[i % band.size])
                setPadding(dp(12), dp(14), 0, dp(14))
                if (i == 39) contentDescription = "scroll_bottom"
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(4) })
        }
        scroll.addView(root)
        setContentView(scroll)
    }
}

class LongPressActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("LongPress page",
            0xFF00897B.toInt(), "LONGPRESS\nWAITING",
            0xFFD81B60.toInt(), "LONGPRESS\nDONE ✔") { stage ->
            val counter = TextView(context).apply { text = "longpresses: 0"; textSize = 16f; contentDescription = "longpress_count" }
            addView(View(context).apply {
                setBackgroundColor(0xFF80CBC4.toInt())
                contentDescription = "longpress_area"
                setOnLongClickListener { counter.bump("longpresses"); stage.fire(); true }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpOf(120)))
            addView(counter)
        }
    }
}

class DoubleTapActivity : Activity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("DoubleTap page",
            0xFF5E35B1.toInt(), "DOUBLETAP\nWAITING",
            0xFF7CB342.toInt(), "DOUBLETAP\nDONE ✔") { stage ->
            val counter = TextView(context).apply { text = "doubletaps: 0"; textSize = 16f; contentDescription = "doubletap_count" }
            val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    counter.bump("doubletaps"); stage.fire(); return true
                }
            })
            addView(View(context).apply {
                setBackgroundColor(0xFFCE93D8.toInt())
                contentDescription = "doubletap_area"
                setOnTouchListener { _, e -> detector.onTouchEvent(e); true }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpOf(120)))
            addView(counter)
        }
    }
}

class BackActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Back page",
            0xFF546E7A.toInt(), "BACK PAGE\nPRESS BACK",
            0xFF546E7A.toInt(), "") { _ ->
            addView(TextView(context).apply {
                text = "Press the system back button to leave this page."
                textSize = 16f
                contentDescription = "back_hint"
            })
        }
    }
}

class OrientationActivity : Activity() {
    private lateinit var label: TextView
    private var stage: Stage? = null
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Orientation page",
            0xFF26A69A.toInt(), "PORTRAIT",
            0xFFEF5350.toInt(), "LANDSCAPE") { s ->
            stage = s
            label = TextView(context).apply { textSize = 16f; contentDescription = "orientation_label" }
            addView(label)
        }
        render(resources.configuration)
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        render(newConfig)
    }
    private fun render(c: Configuration) {
        val land = c.orientation == Configuration.ORIENTATION_LANDSCAPE
        label.text = if (land) "orientation: landscape" else "orientation: portrait"
        if (land) stage?.fire()
    }
}

private fun android.content.Context.dpOf(v: Int) = (v * resources.displayMetrics.density).toInt()
