package com.hisma.app.ui.auth

sealed class AuthNavigationEvent {
    object NavigateToDashboard : AuthNavigationEvent()
    object NavigateToRegisterSelection : AuthNavigationEvent()
    object NavigateToRegisterLubricenter : AuthNavigationEvent()
    object NavigateToRegisterEmployee : AuthNavigationEvent()
    object NavigateToRegister : AuthNavigationEvent()
    object NavigateToLogin : AuthNavigationEvent()
    object NavigateToForgotPassword : AuthNavigationEvent()
}