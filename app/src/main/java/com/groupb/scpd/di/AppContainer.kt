package com.groupb.scpd.di

import com.groupb.scpd.data.AuthRepository
import com.groupb.scpd.data.FirebaseAuthRepository

/** Creates every repository once. Each area adds its own line here. */
class AppContainer {
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
}