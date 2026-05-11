package org.hearth.circuits.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import org.w3c.dom.Element

class NoNetworkPermissionDetector : Detector(), XmlScanner {

    override fun getApplicableElements(): Collection<String> = listOf("uses-permission")

    override fun visitElement(context: XmlContext, element: Element) {
        val name = element.getAttributeNS(
            "http://schemas.android.com/apk/res/android",
            "name"
        )
        if (name in FORBIDDEN_PERMISSIONS) {
            context.report(
                ISSUE,
                element,
                context.getLocation(element),
                "Forbidden network permission `$name` — this app must remain strictly offline."
            )
        }
    }

    companion object {
        private val FORBIDDEN_PERMISSIONS = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.CHANGE_NETWORK_STATE",
            "android.permission.CHANGE_WIFI_STATE"
        )

        val ISSUE: Issue = Issue.create(
            id = "NoInternetAllowed",
            briefDescription = "Network permissions are forbidden in this app",
            explanation = """
                This app operates entirely offline. No network permissions of any kind are allowed.
                Remove the `uses-permission` element declaring a network permission.
            """.trimIndent(),
            category = Category.SECURITY,
            priority = 10,
            severity = Severity.ERROR,
            implementation = Implementation(
                NoNetworkPermissionDetector::class.java,
                Scope.MANIFEST_SCOPE
            )
        )
    }
}
