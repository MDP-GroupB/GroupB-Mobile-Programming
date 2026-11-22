package com.groupb.scpd.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.groupb.scpd.data.AuthRepository
import com.groupb.scpd.data.FirebaseAuthRepository
import com.groupb.scpd.data.local.ScannerDatabase
import com.groupb.scpd.data.scanner.FirestoreScannerRepository
import com.groupb.scpd.data.scanner.ScannerRepository

/** Creates every repository once. Each area adds its own line here. Uses the application context only. */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext   // never an Activity (Storage lecture)
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }

    private val scannerDatabase: ScannerDatabase by lazy {
        ScannerDatabase.getInstance(appContext)
    }

    val scannerRepository: ScannerRepository by lazy {
        FirestoreScannerRepository(
            firestore = FirebaseFirestore.getInstance(),
            scannerDao = scannerDatabase.scannerDao()
        )
    }
}