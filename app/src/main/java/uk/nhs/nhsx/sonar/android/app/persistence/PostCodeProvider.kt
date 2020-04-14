package uk.nhs.nhsx.sonar.android.app.persistence

interface PostCodeProvider {
    fun setPostCode(postCode: String)
    fun getPostCode(): String
}
