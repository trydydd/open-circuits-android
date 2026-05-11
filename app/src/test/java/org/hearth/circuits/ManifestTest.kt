package org.hearth.circuits

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ManifestTest {

    @Test
    fun no_internet_permission() {
        val app = RuntimeEnvironment.getApplication()
        val pi = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
        val perms = pi.requestedPermissions?.toList() ?: emptyList()
        assertFalse("INTERNET must not be declared", perms.contains("android.permission.INTERNET"))
        assertFalse(
            "ACCESS_NETWORK_STATE must not be declared",
            perms.contains("android.permission.ACCESS_NETWORK_STATE")
        )
        assertFalse(
            "ACCESS_WIFI_STATE must not be declared",
            perms.contains("android.permission.ACCESS_WIFI_STATE")
        )
    }

    @Test
    fun cleartext_traffic_disabled() {
        val app = RuntimeEnvironment.getApplication()
        val ai = app.packageManager.getApplicationInfo(app.packageName, 0)
        assertEquals(
            "FLAG_USES_CLEARTEXT_TRAFFIC must not be set",
            0,
            ai.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC
        )
    }

    @Test
    fun backup_disabled() {
        val app = RuntimeEnvironment.getApplication()
        val ai = app.packageManager.getApplicationInfo(app.packageName, 0)
        assertEquals(
            "FLAG_ALLOW_BACKUP must not be set",
            0,
            ai.flags and ApplicationInfo.FLAG_ALLOW_BACKUP
        )
    }

    @Test
    fun launcher_intent_filter() {
        val app = RuntimeEnvironment.getApplication()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = app.packageManager.queryIntentActivities(intent, 0)
        assertEquals("Exactly one LAUNCHER activity must be declared", 1, activities.size)
        assertEquals(
            "MainActivity must be the LAUNCHER activity",
            "${app.packageName}.MainActivity",
            activities[0].activityInfo.name
        )
    }
}
