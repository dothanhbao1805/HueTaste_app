package com.base.common.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.base.common.Config
import com.base.common.util.theme.ColorManager
import com.base.data.Prefs
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber
import javax.inject.Inject

abstract class LsApplication: Application(), LifecycleObserver {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: LsApplication
    }

    init {
        app = this@LsApplication
    }

    @Inject lateinit var config: Config
    @Inject lateinit var prefs: Prefs
    @Inject lateinit var colorManager: ColorManager

    abstract fun onCreated()

    var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()

        // Setup Timber
        Timber.plant(Timber.DebugTree())

        // RxThrowable
        RxJavaPlugins.setErrorHandler { e ->
            Timber.e("Error: $e")
        }

        // Init Lifecycle
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { }

            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityResumed(activity: Activity) { }

            override fun onActivityPaused(activity: Activity) { }

            override fun onActivityStopped(activity: Activity) { }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { }

            override fun onActivityDestroyed(activity: Activity) { }
        })
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        onCreated()
    }

}