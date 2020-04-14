package uk.nhs.nhsx.sonar.android.app

sealed class ViewState {
    object Progress : ViewState()
    object Success : ViewState()
    object Error : ViewState()
}
