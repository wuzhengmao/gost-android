package org.mingy.gost

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
        if ((ACTION == intent.action || "org.mingy.gost.ACTION_START" == intent.action) && auto_start) {
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
        }
    }
}