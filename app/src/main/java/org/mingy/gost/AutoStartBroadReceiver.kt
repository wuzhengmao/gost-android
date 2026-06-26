package org.mingy.gost

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

class AutoStartBroadReceiver : BroadcastReceiver() {
    private val ACTION = "android.intent.action.BOOT_COMPLETED"
    override fun onReceive(context: Context, intent: Intent) {
        //开机启动
        val editor = context.getSharedPreferences("data", AppCompatActivity.MODE_PRIVATE)
        val auto_start = editor.getBoolean(PreferencesKey.AUTO_START, false)
        if ((ACTION == intent.action || "org.mingy.gost.ACTION_START" == intent.action || "org.mingy.gost.ALARM_WAKE" == intent.action) && auto_start) {
            val gostConfigSet = editor.getStringSet(PreferencesKey.AUTO_START_GOST_LIST, emptySet())
            val gostConfigList = gostConfigSet?.map { GostConfig(it) }
            val configList = gostConfigList ?: emptyList()
            if (configList.isEmpty()) return
            //开机启动
            val mainIntent = Intent(context, ShellService::class.java)
            mainIntent.setAction(ShellServiceAction.START)
            mainIntent.putParcelableArrayListExtra(IntentExtraKey.GostConfig, ArrayList(configList))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(mainIntent)
            } else {
                context.startService(mainIntent)
            }
            // 重新设置下一次闹钟（关键）
            scheduleNext(context)
        }
    }

    private fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AutoStartBroadReceiver::class.java).apply {
            action = "org.mingy.gost.ALARM_WAKE"
        }
        val pi = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val time = System.currentTimeMillis() + 15 * 60_000 // 15分钟
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pi
        )
    }
}