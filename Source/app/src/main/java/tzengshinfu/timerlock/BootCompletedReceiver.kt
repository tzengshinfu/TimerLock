package tzengshinfu.timerlock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class BootCompletedReceiver : BroadcastReceiver() {
    private lateinit var timerLockSetting: TimerLockSetting

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                timerLockSetting = TimerLockSetting(context)
                timerLockSetting.startTimerLockService()
            }
        }
    }
}