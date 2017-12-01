package tzengshinfu.timerlock

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.CountDownTimer
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.WindowManager
import kotlinx.android.synthetic.main.view_lock.view.*
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.concurrent.fixedRateTimer


class TimerLockService : Service() {
    //region 屬性
    private lateinit var timerLockSetting: TimerLockSetting
    private var userUnlockKeys: String = ""
    private lateinit var timer_CheckSetting: Timer
    private val checkInterval: Long = 1000

    override fun onCreate() {
        super.onCreate()

        timerLockSetting = TimerLockSetting(this@TimerLockService)
        setTimer_CheckSetting()
    }

    private fun setTimer_CheckSetting() {
        timer_CheckSetting = fixedRateTimer(initialDelay = 0, period = checkInterval) {
            if (timerLockSetting.getIsLockEnabled()) {
                var timeNow = LocalDateTime().toDateTime().millis
                var timeReserveLock = timerLockSetting.getReserveLockMilliSeconds()

                if (timeReserveLock > timeNow) {
                    timerLockSetting.setNotification(timerLockSetting.getNotificationText(timeReserveLock - timeNow))
                } else {
                    timerLockSetting.setNotification(this@TimerLockService.getString(R.string.appName) + this@TimerLockService.getString(R.string.lockingScreen))
                    runOnUiThread({
                        lockScreen()
                    })

                    timer_CheckSetting.cancel()
                }
            } else {
                timerLockSetting.setNotification("")

                timer_CheckSetting.cancel()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timer_CheckSetting.cancel()
        setTimer_CheckSetting()

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun lockScreen() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var layoutParams = WindowManager.LayoutParams()

        val userUnlockKeysDelay: Long = 5000
        var timer_CheckUserUnlockKeysDelay = object : CountDownTimer(userUnlockKeysDelay, userUnlockKeysDelay) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                userUnlockKeys = ""
            }
        }

        //region LockView參數
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            else -> layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams.flags = (WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN  or WindowManager.LayoutParams.FLAG_SECURE)
        layoutParams.format = PixelFormat.OPAQUE
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.gravity = Gravity.LEFT
        layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        //endregion

        var view_lock = LayoutInflater.from(this@TimerLockService).inflate(R.layout.view_lock, null)
        view_lock.setBackgroundColor(timerLockSetting.backgroundColor)
        view_lock.textView_Question.text = timerLockSetting.getUnlockQuestion()
        view_lock.editText_Answer.setTransformationMethod(PasswordTransformationMethod.getInstance())
        view_lock.editText_Answer.setOnKeyListener { _, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        timer_CheckUserUnlockKeysDelay.cancel()
                        userUnlockKeys += timerLockSetting.getKeySymbol(keyCode)
                        timer_CheckUserUnlockKeysDelay.start()

                        if (userUnlockKeys == timerLockSetting.getUnlockKeys()) {
                            windowManager.removeView(view_lock)
                            timer_CheckUserUnlockKeysDelay.cancel()
                            setMute(false)
                            timerLockSetting.setIsLockEnabled(false)
                            timerLockSetting.setIsLocking(false)
                            timerLockSetting.setNotification("")

                            toast(R.string.unlock)
                        }
                    }

                    true
                }
                KeyEvent.KEYCODE_ENTER -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (view_lock.editText_Answer.text.toString() == timerLockSetting.getUnlockAnswer()) {
                            windowManager.removeView(view_lock)
                            setMute(false)
                            timerLockSetting.setIsLockEnabled(false)
                            timerLockSetting.setIsLocking(false)
                            timerLockSetting.setNotification("")

                            toast(R.string.unlock)
                        }
                    }

                    true
                }
                else -> false
            }
        }

        view_lock.editText_Answer.requestFocus()
        windowManager.addView(view_lock, layoutParams)
        setMute(true)
        timerLockSetting.setIsLocking(true)
    }

    private fun setMute(isMute: Boolean) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (isMute) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
        } else {
            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
        }
    }
}