package com.groupb.scpd.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groupb.scpd.data.model.UserProfile
import com.groupb.scpd.data.model.isAdmin
import com.groupb.scpd.data.model.isManager

/** Home after sign-in: shows only the tools this user's role allows (R6). */
@Composable
fun HomeScreen(
    profile: UserProfile,
    onDiscover: () -> Unit,
    onManageEvents: () -> Unit,
    onScanner: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome, ${profile.name}", style = MaterialTheme.typography.headlineSmall)
        if (profile.isAdmin) {
            // Admin has no app screens: approvals and roles are done in the Firebase console.
            Text("Admin tasks are done in the Firebase console.")
        } else {
            Text("Student ID ${profile.maskedId}")
            Button(onClick = onDiscover, modifier = Modifier.fillMaxWidth()) { Text("Discover events") }
            if (profile.isManager) {
                Button(onClick = onManageEvents, modifier = Modifier.fillMaxWidth()) { Text("Manage events") }
            }
            Button(onClick = onScanner, modifier = Modifier.fillMaxWidth()) { Text("Committee tools") }
        }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
    }
}