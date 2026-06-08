package com.erp.pda

import android.app.Application
import com.erp.pda.data.api.SessionManager

class ErpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        SessionManager.init(this)
    }

    companion object {
        lateinit var instance: ErpApplication
            private set
    }
}
