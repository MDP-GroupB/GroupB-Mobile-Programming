package com.groupb.scpd

import android.app.Application
import com.groupb.scpd.di.AppContainer

/** Starts with the app and owns the one AppContainer. */
class ScpdApp : Application() {
    val container by lazy { AppContainer() }
}