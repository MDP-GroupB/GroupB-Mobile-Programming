package com.groupb.scpd.di

import android.content.Context
import com.groupb.scpd.data.AuthRepository
import com.groupb.scpd.data.FirebaseAuthRepository

/** Creates every repository once. Each area adds its own line here. Uses the application context only. */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext   // never an Activity (Storage lecture)
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
}