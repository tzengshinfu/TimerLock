package tzengshinfu.timerlock

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.*
import org.jetbrains.anko.toast
import org.jetbrains.anko.alert
import android.graphics.Paint.UNDERLINE_TEXT_FLAG




class MainActivity : AppCompatActivity() {
    //region 屬性
    private val overlayPermissionRequestCode = 1234

    private lateinit var timerLockSetting: TimerLockSetting
    private var newUnlockKeys: String = ""
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerLockSetting = TimerLockSetting(this@MainActivity)

        switch_LockStatus.isChecked = timerLockSetting.getIsLockEnabled()
        editText_UnlockQuestion.setText(timerLockSetting.getUnlockQuestion())
        editText_UnlockAnswer.setText(timerLockSetting.getUnlockAnswer())
        textView_UnlockKeys.text = timerLockSetting.getUnlockKeys()
        textView_UnlockKeys.setPaintFlags(textView_UnlockKeys.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG)
        numberPicker_ReserveLockMinutes.value = timerLockSetting.getReserveLockMinutes()
        numberPicker_ReserveLockMinutes.maxValue = timerLockSetting.maxReserveLockMinutes
        numberPicker_ReserveLockMinutes.minValue = timerLockSetting.minReserveLockMinutes

        editText_UnlockQuestion.requestFocus()

        button_SaveSetting.setOnClickListener {
            button_SaveSetting.requestFocus()
            saveSetting()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        var result = false

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                result = true

                if (newUnlockKeys.length < timerLockSetting.maxUnlockKeysLength) {
                    newUnlockKeys += timerLockSetting.getKeySymbol(keyCode)
                } else {
                    newUnlockKeys = ""
                }

                textView_UnlockKeys.text = newUnlockKeys
                textView_UnlockKeys.setBackgroundColor(timerLockSetting.backgroundColor)
            }
        }

        return result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == overlayPermissionRequestCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    saveSetting()
                } else {
                    toast(R.string.claimPermissionAgain)

                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
                    startActivityForResult(intent, overlayPermissionRequestCode)
                }
            }
        }
    }

    private fun checkRequiredField() {
        var promptText = ""

        if (editText_UnlockQuestion.text.toString() == "") {
            promptText += this@MainActivity.getString(R.string.notYetEnterUnlockQuestion) + "\n"
            editText_UnlockQuestion.setBackgroundColor(timerLockSetting.highlightColor)
            editText_UnlockQuestion.requestFocus()
        } else {
            editText_UnlockQuestion.setBackgroundColor(timerLockSetting.backgroundColor)
        }

        if (editText_UnlockAnswer.text.toString() == "") {
            promptText += this@MainActivity.getString(R.string.notYetEnterUnlockAnswer) + "\n"
            editText_UnlockAnswer.setBackgroundColor(timerLockSetting.highlightColor)
            editText_UnlockAnswer.requestFocus()
        } else {
            editText_UnlockAnswer.setBackgroundColor(timerLockSetting.backgroundColor)
        }

        if (textView_UnlockKeys.text == "") {
            promptText += this@MainActivity.getString(R.string.notYetEnterUnlockKeys) + "\n"
            textView_UnlockKeys.setBackgroundColor(timerLockSetting.highlightColor)
            textView_UnlockKeys.requestFocus()
        } else {
            textView_UnlockKeys.setBackgroundColor(timerLockSetting.backgroundColor)
        }

        if (promptText == "") {
            confirmSaveSetting()
        } else {
            alert(promptText, this@MainActivity.getString(R.string.set)) { positiveButton(this@MainActivity.getString(R.string.ok)) {} }.show()
        }
    }

    private fun confirmSaveSetting() {
        alert(this@MainActivity.getString(R.string.confirmSaveSetting), this@MainActivity.getString(R.string.set)) {
            positiveButton(this@MainActivity.getString(R.string.ok)) {
                timerLockSetting.setIsLockEnabled(isLockEnabled = switch_LockStatus.isChecked)
                timerLockSetting.setUnlockQuestion(editText_UnlockQuestion.text.toString())
                timerLockSetting.setUnlockAnswer(editText_UnlockAnswer.text.toString())
                timerLockSetting.setUnlockKeys(textView_UnlockKeys.text.toString())
                timerLockSetting.setReserveLockMinutes(numberPicker_ReserveLockMinutes.value)
                timerLockSetting.startTimerLockService()
                newUnlockKeys = ""

                toast(this@MainActivity.getString(R.string.settingSaved))
            }

            negativeButton(this@MainActivity.getString(R.string.cancel)) {

            }
        }.show()
    }

    private fun saveSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this@MainActivity)) {
                checkRequiredField()
            } else {
                toast(R.string.claimPermission)

                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
                startActivityForResult(intent, overlayPermissionRequestCode)
            }
        } else {
            checkRequiredField()
        }
    }
}