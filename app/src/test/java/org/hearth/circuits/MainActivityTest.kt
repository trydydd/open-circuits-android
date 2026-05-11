package org.hearth.circuits

import android.net.Uri
import android.webkit.WebResourceRequest
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    @Test
    fun webview_loads_index() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val shadow = Shadows.shadowOf(activity.webView)
                assertEquals(
                    "https://appassets.androidplatform.net/assets/html/index.html",
                    shadow.lastLoadedUrl
                )
            }
        }
    }

    @Test
    fun dom_storage_enabled() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.webView.settings.domStorageEnabled)
            }
        }
    }

    @Test
    fun javascript_enabled() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.webView.settings.javaScriptEnabled)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun file_access_disabled() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val settings = activity.webView.settings
                assertFalse("allowFileAccess must be false", settings.allowFileAccess)
                assertFalse(
                    "allowFileAccessFromFileURLs must be false",
                    settings.allowFileAccessFromFileURLs
                )
                assertFalse(
                    "allowUniversalAccessFromFileURLs must be false",
                    settings.allowUniversalAccessFromFileURLs
                )
            }
        }
    }

    @Test
    fun media_playback_no_user_gesture_required() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(
                    "mediaPlaybackRequiresUserGesture must be true",
                    activity.webView.settings.mediaPlaybackRequiresUserGesture
                )
            }
        }
    }

    @Test
    fun blocks_non_file_requests() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val request = FakeWebResourceRequest("https://example.com/should/be/blocked")
                val response = activity.webViewClient.shouldInterceptRequest(activity.webView, request)
                assertNotNull("Response must not be null for non-asset host", response)
                assertEquals("Status code must be 403", 403, response!!.statusCode)
            }
        }
    }

    @Test
    fun back_button_uses_web_history() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // On first load there is only one history entry, so canGoBack() is false.
                // Pressing back should finish the activity.
                assertFalse("No back history on first load", activity.webView.canGoBack())
                activity.onBackPressedDispatcher.onBackPressed()
                assertTrue("Activity must finish when web history is empty", activity.isFinishing)
            }
        }
    }

    @Test
    fun back_button_goes_back_in_web_history() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Loading a second URL builds history so canGoBack() becomes true.
                activity.webView.loadUrl(
                    "https://appassets.androidplatform.net/assets/html/DC/DC_1.html"
                )
                assertTrue("Should be able to go back after second load", activity.webView.canGoBack())

                activity.onBackPressedDispatcher.onBackPressed()

                assertFalse("Activity must not finish when navigating back in web history", activity.isFinishing)
            }
        }
    }
}

private class FakeWebResourceRequest(private val urlStr: String) : WebResourceRequest {
    override fun getUrl(): Uri = Uri.parse(urlStr)
    override fun isForMainFrame(): Boolean = true
    override fun isRedirect(): Boolean = false
    override fun hasGesture(): Boolean = false
    override fun getMethod(): String = "GET"
    override fun getRequestHeaders(): Map<String, String> = emptyMap()
}
