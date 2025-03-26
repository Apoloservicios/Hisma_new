package com.hisma.app.ui.auth

sealed class AuthNavigationEvent {
    object NavigateToDashboard : AuthNavigationEvent()
    object NavigateToRegister : AuthNavigationEvent()
    object NavigateToLogin : AuthNavigationEvent()
    object NavigateToForgotPassword : AuthNavigationEvent()
}