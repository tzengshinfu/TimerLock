package tzengshinfu.timerlock

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.support.v7.app.NotificationCompat
import android.view.KeyEvent.KEYCODE_VOLUME_DOWN
import android.view.KeyEvent.KEYCODE_VOLUME_UP
import org.joda.time.*


class TimerLockSetting(context: Context) {
    //region 屬性
    val maxReserveLockMinutes = 120
    val minReserveLockMinutes = 1
    val maxUnlockKeysLength = 7
    val notifyId = 0
    val backgroundColor: Int = Color.rgb(250, 250, 250)
    val highlightColor : Int = Color.YELLOW

    private var appSettings: SharedPreferences
    private var application: Context = context
    //endregion

    init {
        appSettings = application.getSharedPreferences(context.packageName, 0)
    }

    fun getIsLockEnabled(): Boolean = appSettings.getBoolean("IsLockEnabled", false)

    fun getUnlockQuestion(): String = appSettings.getString("UnlockQuestion", "")

    fun getUnlockAnswer(): String = appSettings.getString("UnlockAnswer", "")

    fun getUnlockKeys(): String = appSettings.getString("UnlockKeys", "")

    fun getReserveLockMinutes(): Int = appSettings.getInt("ReserveLockMinutes", 1)

    fun getReserveLockMilliSeconds(): Long = appSettings.getLong("ReserveLockMilliSeconds", 0)

    fun getIsLocking(): Boolean = appSettings.getBoolean("IsLocking", false)

    fun setIsLockEnabled(isLockEnabled: Boolean) {
        val appSettingsEditor = appSettings.edit()
        appSettingsEditor.putBoolean("IsLockEnabled", isLockEnabled)
        appSettingsEditor.commit()
    }

    fun setUnlockQuestion(unlockQuestion: String) {
        val appSettingsEditor = appSettings.edit()
        appSettingsEditor.putString("UnlockQuestion", unlockQuestion)
        appSettingsEditor.commit()
    }

    fun setUnlockAnswer(unlockAnswer: String) {
        val appSettingsEditor = appSettings.edit()
        appSettingsEditor.putString("UnlockAnswer", unlockAnswer)
        appSettingsEditor.commit()
    }

    fun setUnlockKeys(unlockKeys: String) {
        val appSettingsEditor = appSettings.edit()
        appSettingsEditor.putString("UnlockKeys", unlockKeys)
        appSettingsEditor.commit()
    }

    fun setReserveLockMinutes(reserveLockMinutes: Int) {
        val appSettingsEditor = appSettings.edit()
        appSettingsEditor.putInt("ReserveLockMinutes", reserveLockMinutes)
        appSettingsEditor.commit()

        setReserveLockMilliSeconds(reserveLockMinutes)
    }

    private fun setReserveLockMilliSeconds(reserveLockMinutes: Int) {
        val appSettingsEditor = appSettings.edit()
        appSettingsEditor.putLong("ReserveLockMilliSeconds", getCalculateLockMilliSeconds(reserveLockMinutes))
        appSettingsEditor.commit()
    }

    fun setIsLocking(isLocking: Boolean) {
        val appSettingsEditor = appSettings.edit()
        appSettingsEditor.putBoolean("IsLocking", isLocking)
        appSettingsEditor.commit()
    }

    /**
     * 將音量鍵常數轉符號以利儲存
     */
    fun getKeySymbol(keyCode: Int): String {
        var result = ""

        when (keyCode) {
            KEYCODE_VOLUME_UP -> result = application.getString(R.string.keycodeValueUpSymbol)
            KEYCODE_VOLUME_DOWN -> result = application.getString(R.string.keycodeValueDownSymbol)
        }

        return result
    }

    private fun getCalculateLockMilliSeconds(addMinute: Int): Long {
        val localDateTime = LocalDateTime()
        var calculateLockTime = localDateTime.plusMinutes(addMinute).toDateTime().millis

        return calculateLockTime
    }

    fun startTimerLockService() {
        val timerLockService = Intent()
        timerLockService.setClass(application, TimerLockService::class.java)
        application.startService(timerLockService)
    }

    fun getNotificationText(duringTimeMilliSeconds: Long): String {
        var duringTimeSeconds = (duringTimeMilliSeconds / 1000)
        var duringTimeMinutes = (duringTimeSeconds / 60)
        var notificationText = ""

        if (duringTimeMinutes > 0) {
            notificationText = duringTimeMinutes.toString() + application.getString(R.string.minute) + application.getString(R.string.laterLock)
        } else {
            notificationText = duringTimeSeconds.toString() + application.getString(R.string.second) + application.getString(R.string.laterLock)
        }

        return notificationText
    }

    fun setNotification(notificationTitle: String) {
        val notification = NotificationCompat.Builder(application)
                .setContentTitle(application.getString(R.string.appName))
                .setSmallIcon(R.mipmap.ic_stat_notify)
                .setContentText(notificationTitle)
                .build()
        notification.flags = Notification.FLAG_ONGOING_EVENT

        val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationTitle != "") {
            notificationManager.notify(notifyId, notification)
        } else {
            notificationManager.cancel(notifyId)
        }
    }
}