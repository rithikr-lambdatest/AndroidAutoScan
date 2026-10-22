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
// One page per Appium endpoint (/click, /tap, /value, /clear, /swipe,
// /scroll, /longpress, /doubletap, /flick, /actions, /back, /orientation);
// each page hosts the element the gesture is performed on, a live counter
// proving the gesture landed, and ONE unlabeled ImageButton so every
// triggered scan has an identifiable finding on that page.
// =====================================================================

private fun Activity.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

/** Standard page skeleton: title, gesture area via [content], counter label, violation. */
private fun Activity.page(title: String, counterId: String? = null,
                          content: LinearLayout.(TextView?) -> Unit) {
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        fitsSystemWindows = true
        setPadding(dp(16), dp(16), dp(16), dp(16))
    }
    root.addView(TextView(this).apply {
        text = title
        textSize = 22f
        setPadding(0, 0, 0, dp(12))
    })
    val counter = counterId?.let {
        TextView(this).apply {
            text = "count: 0"
            textSize = 16f
            contentDescription = "gesture counter"
            tag = it
            accessibilityPaneTitle = null
        }
    }
    root.content(counter)
    counter?.let { c ->
        c.setTag(c.tag); c.id = View.generateViewId()
        root.addView(c)
    }
    // Per-page violation: unlabeled image button -> missing contentDescription.
    root.addView(ImageButton(this).apply {
        setImageResource(android.R.drawable.ic_menu_info_details)
        tag = "violation_missing_label"
    }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { topMargin = dp(16) })
    setContentView(root)
}

/** Counter helper — resource-id isn't available for programmatic views, so pages set
 *  the Appium-visible identity via contentDescription on interactive targets and
 *  readable text for assertions. */
private fun TextView.bump(prefix: String = "count") {
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
        page("Click page") { _ ->
            val counter = TextView(context).apply { text = "clicks: 0"; textSize = 16f; contentDescription = "click_count" }
            addView(Button(context).apply {
                text = "Click me"
                isAllCaps = false
                contentDescription = "btn_click"
                setOnClickListener { counter.bump("clicks") }
            })
            addView(counter)
        }
    }
}

class TapActivity : Activity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Tap page") { _ ->
            val counter = TextView(context).apply { text = "taps: 0"; textSize = 16f; contentDescription = "tap_count" }
            addView(View(context).apply {
                setBackgroundColor(0xFFB3C7E6.toInt())
                contentDescription = "tap_area"
                setOnClickListener { counter.bump("taps") }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpOf(120)))
            addView(counter)
        }
    }
}

class ValueActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Value page") { _ ->
            val echo = TextView(context).apply { text = "typed: "; textSize = 16f; contentDescription = "value_echo" }
            addView(EditText(context).apply {
                hint = "Type here"
                contentDescription = "input_value"
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) { echo.text = "typed: ${s}" }
                    override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                })
            })
            addView(echo)
        }
    }
}

class ClearActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Clear page") { _ ->
            addView(EditText(context).apply {
                setText("Clear this text")
                contentDescription = "input_clear"
            })
        }
    }
}

/** Shared base for the drag-family pages (swipe / flick / actions). */
abstract class TouchCounterActivity(
    private val title: String,
    private val areaDesc: String,
    private val counterDesc: String,
    private val counterPrefix: String,
) : Activity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page(title) { _ ->
            val counter = TextView(context).apply { text = "$counterPrefix: 0"; textSize = 16f; contentDescription = counterDesc }
            addView(View(context).apply {
                setBackgroundColor(0xFFCBE6B3.toInt())
                contentDescription = areaDesc
                var downX = 0f; var downY = 0f
                setOnTouchListener { _, e ->
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y; true }
                        MotionEvent.ACTION_UP -> {
                            if (kotlin.math.abs(e.x - downX) > 80 || kotlin.math.abs(e.y - downY) > 80 || countTouchesToo())
                                counter.bump(counterPrefix)
                            true
                        }
                        else -> true
                    }
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpOf(200)))
            addView(counter)
        }
    }
    open fun countTouchesToo() = false
}

class SwipeActivity : TouchCounterActivity("Swipe page", "swipe_area", "swipe_count", "swipes")
class FlickActivity : TouchCounterActivity("Flick page", "flick_area", "flick_count", "flicks")
class ActionsActivity : TouchCounterActivity("Actions page", "actions_area", "actions_count", "gestures") {
    override fun countTouchesToo() = true   // plain pointer sequences count as well
}

class ScrollActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val scroll = ScrollView(this).apply { fitsSystemWindows = true; contentDescription = "scroll_container" }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        root.addView(TextView(this).apply { text = "Scroll page"; textSize = 22f; setPadding(0, 0, 0, dp(12)) })
        root.addView(ImageButton(this).apply { setImageResource(android.R.drawable.ic_menu_info_details) },
            LinearLayout.LayoutParams(dp(48), dp(48)))
        repeat(40) { i ->
            root.addView(TextView(this).apply {
                text = "Row ${i + 1}"
                textSize = 16f
                setPadding(0, dp(10), 0, dp(10))
                if (i == 39) contentDescription = "scroll_bottom"
            })
        }
        scroll.addView(root)
        setContentView(scroll)
    }
}

class LongPressActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("LongPress page") { _ ->
            val counter = TextView(context).apply { text = "longpresses: 0"; textSize = 16f; contentDescription = "longpress_count" }
            addView(View(context).apply {
                setBackgroundColor(0xFFE6CBB3.toInt())
                contentDescription = "longpress_area"
                setOnLongClickListener { counter.bump("longpresses"); true }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpOf(150)))
            addView(counter)
        }
    }
}

class DoubleTapActivity : Activity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("DoubleTap page") { _ ->
            val counter = TextView(context).apply { text = "doubletaps: 0"; textSize = 16f; contentDescription = "doubletap_count" }
            val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean { counter.bump("doubletaps"); return true }
            })
            addView(View(context).apply {
                setBackgroundColor(0xFFD9B3E6.toInt())
                contentDescription = "doubletap_area"
                setOnTouchListener { _, e -> detector.onTouchEvent(e); true }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dpOf(150)))
            addView(counter)
        }
    }
}

class BackActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Back page") { _ ->
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
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        page("Orientation page") { _ ->
            label = TextView(context).apply {
                textSize = 16f
                contentDescription = "orientation_label"
            }
            addView(label)
        }
        render(resources.configuration)
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        render(newConfig)
    }
    private fun render(c: Configuration) {
        label.text = if (c.orientation == Configuration.ORIENTATION_LANDSCAPE) "orientation: landscape" else "orientation: portrait"
    }
}

// LinearLayout content closures run with the layout as receiver; expose dp via its context.
private fun android.content.Context.dpOf(v: Int) = (v * resources.displayMetrics.density).toInt()
private val LinearLayout.context2: android.content.Context get() = context
