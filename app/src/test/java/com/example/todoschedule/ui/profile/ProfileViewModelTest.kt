package com.example.todoschedule.ui.profile

import android.os.Build
import app.cash.turbine.test
import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.UserRepository
import com.example.todoschedule.domain.use_case.profile.GetUserProfileUseCase
import com.example.todoschedule.domain.use_case.profile.LogoutUseCase
import com.example.todoschedule.domain.utils.Resource
import com.example.todoschedule.ui.profile.model.EditField
import com.example.todoschedule.ui.profile.model.ProfileEvent
import com.example.todoschedule.ui.profile.model.ProfileUiState
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule(testDispatcher)

    @RelaxedMockK
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase

    @RelaxedMockK
    private lateinit var logoutUseCase: LogoutUseCase

    @RelaxedMockK
    private lateinit var userRepository: UserRepository

    private lateinit var viewModel: ProfileViewModel

    private val initialUser =
        User(id = 1, username = "testuser", email = "test@example.com", phoneNumber = "1234567890")

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        coEvery { getUserProfileUseCase() } returns flowOf(Resource.Success(initialUser))
        viewModel = ProfileViewModel(getUserProfileUseCase, logoutUseCase, userRepository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `init loads profile and updates state on success`() = runTest(testDispatcher) {
        // Given
        val mockUser = User(id = 1, username = "testuser", email = "test@example.com")
        coEvery { getUserProfileUseCase() } returns flow {
            emit(Resource.Loading())
            emit(Resource.Success(mockUser))
        }

        // When
        viewModel = ProfileViewModel(getUserProfileUseCase, logoutUseCase, userRepository)

        // Then
        viewModel.uiState.test {
            // 1. Initial state (default ProfileUiState from StateFlow's initial value)
            assertEquals("Initial state should be default", ProfileUiState(), awaitItem())

            // 2. Loading state (emitted by getUserProfileUseCase via loadUserProfile in init)
            val loadingState = awaitItem()
            assertTrue("State should be loading. Actual: $loadingState", loadingState.isLoading)
            assertNull(
                "User should be null during loading. Actual: ${loadingState.user}",
                loadingState.user
            )
            assertNull(
                "Error should be null during loading. Actual: ${loadingState.error}",
                loadingState.error
            )

            // 3. Success state (emitted by getUserProfileUseCase)
            val successState = awaitItem()
            assertFalse(
                "State should not be loading after success. Actual: $successState",
                successState.isLoading
            )
            assertEquals(
                "User should match mockUser. Actual: ${successState.user}",
                mockUser,
                successState.user
            )
            assertNull(
                "Error should be null on success. Actual: ${successState.error}",
                successState.error
            )

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `init loads profile and updates state on error`() = runTest(testDispatcher) {
        // Given
        val errorMessage = "Failed to load user"
        coEvery { getUserProfileUseCase() } returns flow {
            emit(Resource.Loading())
            emit(Resource.Error(errorMessage))
        }

        // When
        viewModel = ProfileViewModel(getUserProfileUseCase, logoutUseCase, userRepository)

        // Then
        viewModel.uiState.test {
            // 1. Initial state
            assertEquals("Initial state should be default", ProfileUiState(), awaitItem())

            // 2. Loading state
            val loadingState = awaitItem()
            assertTrue("State should be loading. Actual: $loadingState", loadingState.isLoading)
            assertNull(
                "User should be null during loading. Actual: ${loadingState.user}",
                loadingState.user
            )
            assertNull(
                "Error should be null during loading. Actual: ${loadingState.error}",
                loadingState.error
            )

            // 3. Error state
            val errorState = awaitItem()
            assertFalse(
                "State should not be loading after error. Actual: $errorState",
                errorState.isLoading
            )
            assertEquals(
                "Error message should match. Actual: ${errorState.error}",
                errorMessage,
                errorState.error
            )
            assertNull("User should be null on error. Actual: ${errorState.user}", errorState.user)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `SaveEditedField with invalid email shows validation error`() = runTest(testDispatcher) {
        // mockkStatic for Patterns is removed as Robolectric will provide a working version.
        // No need for:
        // mockkStatic(android.util.Patterns::class)
        // val mockPattern = mockk<Pattern>()
        // val mockMatcher = mockk<Matcher>()
        // every { android.util.Patterns.EMAIL_ADDRESS } returns mockPattern
        // every { mockPattern.matcher(any()) } returns mockMatcher
        // every { mockMatcher.matches() } returns false

        // try/finally for unmockkStatic is also removed.

        viewModel.uiState.test {
            assertEquals(
                "Initial state after setUp",
                ProfileUiState(user = initialUser),
                awaitItem()
            )

            viewModel.handleEvent(ProfileEvent.SaveEditedField(EditField.EMAIL, "invalid-email"))
            advanceUntilIdle() // Ensure event processing coroutine runs

            val savingState = awaitItem()
            assertTrue("Should be saving", savingState.isSaving)

            val errorState = awaitItem()
            assertFalse("Should not be saving after error", errorState.isSaving)
            assertEquals("邮箱格式不正确", errorState.error) // This assertion should now pass
            assertEquals("User data should not be corrupted", initialUser, errorState.user)
            coVerify(exactly = 0) { userRepository.updateUser(any()) }
            cancelAndConsumeRemainingEvents()
        }
    }

    // TODO: Add more tests for other events and scenarios:
    // - StartEditField event
    // - SaveEditedField event (success, validation errors, repository errors)
    // - CancelEdit event
    // - Logout event (success, failure)
    // - ClearMessages event
} 