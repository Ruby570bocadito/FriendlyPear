package app.friendlypear.ui.peri

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import app.friendlypear.R

/** Peri's home screen. The big talk button and Peri's states arrive with the voice and UI tasks. */
class PeriActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = getString(R.string.peri_greeting)
                textSize = 28f
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            },
        )
    }
}
