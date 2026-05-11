package org.hearth.circuits.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class NoNetworkPermissionIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(NoNetworkPermissionDetector.ISSUE)
    override val api: Int = CURRENT_API
    override val vendor: Vendor = Vendor(
        vendorName = "Open Circuits Android",
        identifier = "org.hearth.circuits:lint-checks"
    )
}
