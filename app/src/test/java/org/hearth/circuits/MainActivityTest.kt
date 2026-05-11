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
                val shadow = Shadows.shadowOf(activity.webView)

                // Default state: canGoBack() is false (no prior history).
                assertFalse("No back history on first load", activity.webView.canGoBack())
                val goBackBefore = shadow.goBackInvocations

                activity.onBackPressedDispatcher.onBackPressed()

                assertEquals("goBack() must not be called when history is empty", goBackBefore, shadow.goBackInvocations)
                assertTrue("Activity must finish when web history is empty", activity.isFinishing)
            }
        }
    }

    @Test
    fun back_button_goes_back_in_web_history() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val shadow = Shadows.shadowOf(activity.webView)

                // Use the shadow API to put the WebView into a "can go back" state.
                @Suppress("DEPRECATION")
                shadow.setCanGoBack(true)
                assertTrue("canGoBack must be true after setup", activity.webView.canGoBack())

                val goBackBefore = shadow.goBackInvocations
                activity.onBackPressedDispatcher.onBackPressed()

                assertEquals("goBack() must be called exactly once", goBackBefore + 1, shadow.goBackInvocations)
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
