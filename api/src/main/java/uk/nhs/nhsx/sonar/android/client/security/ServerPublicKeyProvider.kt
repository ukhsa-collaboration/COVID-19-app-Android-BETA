/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.client.security

import java.security.PublicKey

interface ServerPublicKeyProvider {
    fun providePublicKey(): PublicKey
}
