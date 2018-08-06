/*
 * Copyright 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.plaidapp.core.designernews.ui.login

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.plaidapp.R
import io.plaidapp.core.data.Result
import io.plaidapp.core.designernews.data.login.LoginRepository
import io.plaidapp.core.designernews.data.users.model.User
import io.plaidapp.core.util.event.Event
import io.plaidapp.designernews.ui.login.LoginUiModel
import io.plaidapp.designernews.ui.login.LoginViewModel
import io.plaidapp.designernews.ui.login.SuccessLoginUiModel
import io.plaidapp.test.shared.LiveDataTestUtil
import io.plaidapp.test.shared.provideFakeCoroutinesContextProvider
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Class that tests [LoginViewModel] by mocking all the dependencies.
 */
class LoginViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val username = "Plaid"
    private val password = "design"
    private val initialUiModel = LoginUiModel(
        false,
        null,
        null,
        false
    )

    private val loginRepo: LoginRepository = mock()

    @Test
    fun login_whenUserLoggedInSuccessfully() = runBlocking {
        val viewModel = LoginViewModel(loginRepo, provideFakeCoroutinesContextProvider())

        // Given that the repository returns a user
        val user = User(
            id = 3,
            firstName = "Plaida",
            lastName = "Plaidich",
            displayName = "Plaida Plaidich",
            portraitUrl = "www"
        )

        whenever(loginRepo.login(username, password)).thenReturn(Result.Success(user))

        // When logging in
        viewModel.login(username, password)

        // Then the correct UI model is created
        val expected = LoginUiModel(
            false,
            null,
            Event(SuccessLoginUiModel("plaida plaidich", "www")),
            false
        )
        val event = LiveDataTestUtil.getValue(viewModel.uiState)
        assertEquals(expected, event)
    }

    @Test
    fun login_whenUserLogInFailed() = runBlocking {
        val viewModel = LoginViewModel(loginRepo, provideFakeCoroutinesContextProvider())

        // Given that the repository returns with error
        whenever(loginRepo.login(username, password))
            .thenReturn(Result.Error(IOException("Login error")))

        // When logging in
        viewModel.login(username, password)

        // Then the correct UI model is created
        val expectedUiModel = LoginUiModel(
            false,
            Event(R.string.login_failed),
            null,
            true
        )
        val event = LiveDataTestUtil.getValue(viewModel.uiState)
        assertEquals(expectedUiModel, event)
    }

    @Test
    fun init_disablesLogin() = runBlocking {
        // When the view model is created
        val viewModel = LoginViewModel(loginRepo, provideFakeCoroutinesContextProvider())

        // Then the login is disabled
        val event = LiveDataTestUtil.getValue(viewModel.uiState)
        assertEquals(initialUiModel, event)
    }

    @Test
    fun loginDataChanged_withValidLogin() = runBlocking {
        // Given a view model
        val viewModel = LoginViewModel(loginRepo, provideFakeCoroutinesContextProvider())

        // When login data changed with valid login data
        viewModel.loginDataChanged(username, password)

        // Then the login is enabled
        val expectedUiModel = LoginUiModel(
            false,
            null,
            null,
            true
        )
        val event = LiveDataTestUtil.getValue(viewModel.uiState)
        assertEquals(expectedUiModel, event)
    }

    @Test
    fun loginDataChanged_withEmptyUsername() = runBlocking {
        // Given a view model
        val viewModel = LoginViewModel(loginRepo, provideFakeCoroutinesContextProvider())

        // When login data changed with invalid login data
        viewModel.loginDataChanged("", password)

        // Then the login is disabled
        val expectedUiModel = LoginUiModel(
            false,
            null,
            null,
            false
        )
        val event = LiveDataTestUtil.getValue(viewModel.uiState)
        assertEquals(expectedUiModel, event)
    }

    @Test
    fun loginDataChanged_withEmptyPassword() = runBlocking {
        // Given a view model
        val viewModel = LoginViewModel(loginRepo, provideFakeCoroutinesContextProvider())

        // When login data changed with invalid login data
        viewModel.loginDataChanged(username, "")

        // Then the login is disabled
        val expectedUiModel = LoginUiModel(
            false,
            null,
            null,
            false
        )
        val event = LiveDataTestUtil.getValue(viewModel.uiState)
        assertEquals(expectedUiModel, event)
    }

    @Test
    fun login_withEmptyUsername() = runBlocking {
        // Given a view model
        val viewModel = LoginViewModel(loginRepo, provideFakeCoroutinesContextProvider())

        // When logging in with invalid login data
        viewModel.login("", password)

        // Then login is not triggered
        verify(loginRepo, never()).login(username, "")
        // Then the UI state is the initial state
        val event = LiveDataTestUtil.getValue(viewModel.uiState)
        assertEquals(initialUiModel, event)
    }

    @Test
    fun login_withEmptyPassword() = runBlocking {
        // Given a view model
        val viewModel = LoginViewModel(loginRepo, provideFakeCoroutinesContextProvider())

        // When logging in with invalid login data
        viewModel.loginDataChanged(username, "")

        // Then login is not triggered
        verify(loginRepo, never()).login(username, "")
        // Then the UI state is the initial state
        val event = LiveDataTestUtil.getValue(viewModel.uiState)
        assertEquals(initialUiModel, event)
    }

    @Test
    fun signup() = runBlocking {
        // Given a view model
        val viewModel = LoginViewModel(loginRepo, provideFakeCoroutinesContextProvider())

        // When signing up
        viewModel.signup()

        // Then an oper url event is emitted
        val event = LiveDataTestUtil.getValue(viewModel.openUrl)
        assertNotNull(event)
    }
}
