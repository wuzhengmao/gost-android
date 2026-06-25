package org.mingy.gost

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class EmptyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        moveTaskToBack(true)

        // 如果你想顺便触发初始化，可以在这里做
        sendBroadcast(Intent(this, AutoStartBroadReceiver::class.java).apply {
            action = "org.mingy.gost.ACTION_START"
        })

        // 立刻退出（不显示UI）
        finish()
    }
}