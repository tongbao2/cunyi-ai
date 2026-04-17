package com.cunyi.doctor

import android.app.Application

class CunyiDoctorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CunyiDoctorApp
            private set
    }
}
