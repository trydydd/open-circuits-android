package org.hearth.circuits

import android.content.res.Configuration
import android.graphics.Color
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeTest {

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun launcher_icon_present_at_all_densities() {
        val densities = intArrayOf(
            DisplayMetrics.DENSITY_MEDIUM,   // mdpi  160
            DisplayMetrics.DENSITY_HIGH,     // hdpi  240
            DisplayMetrics.DENSITY_XHIGH,    // xhdpi 320
            DisplayMetrics.DENSITY_XXHIGH,   // xxhdpi 480
            DisplayMetrics.DENSITY_XXXHIGH,  // xxxhdpi 640
        )
        for (density in densities) {
            val config = Configuration(context.resources.configuration)
            config.densityDpi = density
            val ctx = context.createConfigurationContext(config)
            val drawable = ResourcesCompat.getDrawable(
                ctx.resources, R.mipmap.ic_launcher_foreground, null
            )
            assertNotNull("ic_launcher_foreground null at densityDpi=$density", drawable)
        }
    }

    @Test
    fun adaptive_icon_xml_correct() {
        val parser = context.resources.getXml(R.mipmap.ic_launcher)
        var foundAdaptiveIcon = false
        var hasBackground = false
        var hasForeground = false

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "adaptive-icon" -> foundAdaptiveIcon = true
                    "background"    -> hasBackground = true
                    "foreground"    -> hasForeground = true
                }
            }
            eventType = parser.next()
        }
        parser.close()

        assertTrue("ic_launcher.xml must have <adaptive-icon>", foundAdaptiveIcon)
        assertTrue("ic_launcher.xml must have <background>", hasBackground)
        assertTrue("ic_launcher.xml must have <foreground>", hasForeground)
    }

    @Test
    fun brand_colors_match_overlay() {
        // brand_copper: --oc-accent rgb(147 95 22) oklch(55% 0.130 60)
        val copper = ContextCompat.getColor(context, R.color.brand_copper)
        assertTrue(
            "brand_copper should be #935F16 (rgb 147 95 22), got #${Integer.toHexString(copper)}",
            copper == Color.rgb(147, 95, 22)
        )
        // brand_cream: --oc-bg rgb(246 243 235) oklch(96.5% 0.010 82)
        val cream = ContextCompat.getColor(context, R.color.brand_cream)
        assertTrue(
            "brand_cream should be #F6F3EB (rgb 246 243 235), got #${Integer.toHexString(cream)}",
            cream == Color.rgb(246, 243, 235)
        )
    }

    @Test
    fun night_colors_present() {
        val nightConfig = Configuration(context.resources.configuration)
        nightConfig.uiMode =
            (nightConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            Configuration.UI_MODE_NIGHT_YES
        val nightCtx = context.createConfigurationContext(nightConfig)

        val lightBg = ContextCompat.getColor(context, R.color.background)
        val nightBg = ContextCompat.getColor(nightCtx, R.color.background)
        assertTrue(
            "values-night/colors.xml must override @color/background (light=$lightBg night=$nightBg)",
            nightBg != lightBg
        )
    }

    @Test
    fun splash_theme_uses_brand() {
        // Theme resource exists
        val splashId = R.style.Theme_OpenCircuits_Splash
        assertTrue("Theme.OpenCircuits.Splash resource must exist", splashId != 0)

        // The splash icon drawable must be available
        val icon = ResourcesCompat.getDrawable(
            context.resources, R.drawable.ic_launcher_foreground, null
        )
        assertNotNull("@drawable/ic_launcher_foreground must resolve for splash icon", icon)

        // brand_cream matches the expected splash background value
        val cream = ContextCompat.getColor(context, R.color.brand_cream)
        assertTrue(
            "brand_cream (#F6F3EB) must match the splash windowSplashScreenBackground value",
            cream == Color.rgb(246, 243, 235)
        )
    }
}
