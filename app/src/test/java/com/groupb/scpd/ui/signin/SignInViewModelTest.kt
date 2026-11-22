package com.groupb.scpd.ui.signin

import com.groupb.scpd.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class SignInViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = FakeAuthRepository()
    private val vm = SignInViewModel(repo)

    private fun fillIn() {
        vm.onEmailChange("student@test.com")
        vm.onPasswordChange("secret123")
    }

    @Test
    fun `empty fields show an error without calling Firebase`() = runTest {
        vm.signIn()
        assertEquals(SignInViewModel.MSG_WRONG_DETAILS, vm.state.value.error)
        assertEquals(0, repo.signInCalls)
    }

    @Test
    fun `correct details load the profile`() = runTest {
        fillIn()
        vm.signIn()
        val state = vm.state.value
        assertEquals(repo.profileToReturn, state.profile)
        assertFalse(state.loading)
        assertNull(state.error)
    }

    @Test
    fun `wrong password shows the S1 message`() = runTest {
        repo.errorToThrow = IllegalStateException("bad password")
        fillIn()
        vm.signIn()
        assertEquals(SignInViewModel.MSG_WRONG_DETAILS, vm.state.value.error)
        assertNull(vm.state.value.profile)
    }

    @Test
    fun `no internet shows the offline message`() = runTest {
        repo.errorToThrow = IOException("No connection")
        fillIn()
        vm.signIn()
        assertEquals(SignInViewModel.MSG_OFFLINE, vm.state.value.error)
    }

    @Test
    fun `double tap signs in only once`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repo.gate = gate
        fillIn()
        vm.signIn()
        vm.signIn()   // second tap while the first is still loading
        assertTrue(vm.state.value.loading)
        gate.complete(Unit)
        assertEquals(1, repo.signInCalls)
        assertNotNull(vm.state.value.profile)
    }

    @Test
    fun `typing clears an old error`() = runTest {
        vm.signIn()   // empty fields: error shown
        vm.onEmailChange("s")
        assertNull(vm.state.value.error)
    }

    @Test
    fun `sign out clears the profile`() = runTest {
        fillIn()
        vm.signIn()
        vm.signOut()
        assertNull(vm.state.value.profile)
        assertEquals(1, repo.signOutCalls)
    }
}