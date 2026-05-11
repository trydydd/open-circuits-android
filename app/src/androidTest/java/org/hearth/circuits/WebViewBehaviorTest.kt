package org.hearth.circuits

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class WebViewBehaviorTest {

    @Test
    fun launches_and_renders() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val latch = CountDownLatch(1)
            val title = AtomicReference<String>()

            scenario.onActivity { activity ->
                activity.webView.post {
                    activity.webView.evaluateJavascript("document.title") { value ->
                        title.set(value?.trim('"') ?: "")
                        latch.countDown()
                    }
                }
            }

            assertTrue("Page should render within 5 seconds", latch.await(5, TimeUnit.SECONDS))
            assertTrue("Title should be non-empty", title.get()?.isNotEmpty() == true)
        }
    }

    @Test
    fun navigation_link_works() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Click the first DC_1 link then poll for the URL change.
            scenario.onActivity { activity ->
                activity.webView.post {
                    activity.webView.evaluateJavascript(
                        "document.querySelector('a[href*=\"DC_1\"]')?.click()"
                    ) { }
                }
            }

            val urlLatch = CountDownLatch(1)
            val url = AtomicReference<String>()
            val deadline = System.currentTimeMillis() + 3_000L

            while (System.currentTimeMillis() < deadline) {
                val checkLatch = CountDownLatch(1)
                scenario.onActivity { activity ->
                    activity.webView.post {
                        activity.webView.evaluateJavascript("document.URL") { value ->
                            url.set(value?.trim('"') ?: "")
                            checkLatch.countDown()
                        }
                    }
                }
                checkLatch.await(1, TimeUnit.SECONDS)
                if (url.get()?.endsWith("DC_1.html") == true) {
                    urlLatch.countDown()
                    break
                }
                Thread.sleep(200)
            }

            assertTrue("URL should end with DC_1.html within 3 seconds", urlLatch.await(0, TimeUnit.SECONDS))
        }
    }

    @Test
    fun localstorage_persists_across_restart() {
        val setLatch = CountDownLatch(1)

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            activity.webView.post {
                activity.webView.evaluateJavascript(
                    "localStorage.setItem('oc:test','v1')"
                ) { setLatch.countDown() }
            }
        }
        assertTrue("localStorage write should complete", setLatch.await(5, TimeUnit.SECONDS))

        scenario.recreate()

        val getLatch = CountDownLatch(1)
        val stored = AtomicReference<String>()

        scenario.onActivity { activity ->
            activity.webView.post {
                activity.webView.evaluateJavascript(
                    "localStorage.getItem('oc:test')"
                ) { value ->
                    stored.set(value?.trim('"') ?: "")
                    getLatch.countDown()
                }
            }
        }
        assertTrue("localStorage read should complete", getLatch.await(5, TimeUnit.SECONDS))
        assertEquals("v1", stored.get())

        scenario.close()
    }
}
