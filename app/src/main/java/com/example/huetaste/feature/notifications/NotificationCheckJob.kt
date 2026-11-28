package com.example.huetaste.feature.notifications

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.usecase.GetAuthStateUseCase
import com.example.huetaste.domain.usecase.GetUnshownNotificationsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Asks the backend for new notifications and shows them in the status bar. Asking also makes the backend create
 * the reminders that are due (meal time, lunar day, daily weather), which is why it runs on a schedule.
 */
@AndroidEntryPoint
class NotificationCheckJob : JobService() {
    @Inject lateinit var getAuthState: GetAuthStateUseCase
    @Inject lateinit var getUnshownNotifications: GetUnshownNotificationsUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var check: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        check = scope.launch {
            try {
                val canShow = NotificationManagerCompat.from(this@NotificationCheckJob).areNotificationsEnabled()
                // Taking marks them shown, so notifications the user blocked are left for a later permission grant.
                if (canShow && getAuthState() == AuthState.SIGNED_IN) {
                    SystemNotifications.show(this@NotificationCheckJob, getUnshownNotifications())
                }
            } catch (e: AppError) {
                // Offline or signed out on the backend: the next run tries again.
            }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        check?.cancel()
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

/** Schedules [NotificationCheckJob] while a user is signed in. */
object NotificationScheduler {
    private const val PERIODIC_JOB_ID = 4101
    private const val NOW_JOB_ID = 4102

    /** Every 15 minutes (the shortest period Android allows) with network, kept across reboots, plus once right now. */
    fun start(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        val job = ComponentName(context, NotificationCheckJob::class.java)
        if (scheduler.getPendingJob(PERIODIC_JOB_ID) == null) {
            scheduler.schedule(
                JobInfo.Builder(PERIODIC_JOB_ID, job)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(15))
                    .setPersisted(true)
                    .build(),
            )
        }
        scheduler.schedule(JobInfo.Builder(NOW_JOB_ID, job).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build())
    }

    /** After signing out: no more checks, and nothing of the previous user left in the status bar. */
    fun stop(context: Context) {
        context.getSystemService(JobScheduler::class.java)?.apply {
            cancel(PERIODIC_JOB_ID)
            cancel(NOW_JOB_ID)
        }
        NotificationManagerCompat.from(context).cancelAll()
    }
}
