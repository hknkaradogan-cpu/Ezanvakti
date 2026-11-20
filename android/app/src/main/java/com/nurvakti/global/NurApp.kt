package com.nurvakti.global

import android.app.Application
import com.nurvakti.global.alarm.Channels
import com.nurvakti.global.alarm.DailyWorker

class NurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Channels.create(this)
        DailyWorker.ensure(this)
    }
}
