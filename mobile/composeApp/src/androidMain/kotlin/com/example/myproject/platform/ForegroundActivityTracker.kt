package com.example.myproject.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the resumed Activity so SDKs that need one (Play Billing's purchase sheet) can get it
 * without the Activity resolving those SDKs eagerly at launch.
 */
class ForegroundActivityTracker : Application.ActivityLifecycleCallbacks {
    private var resumed: WeakReference<Activity>? = null

    val currentActivity: Activity? get() = resumed?.get()

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
