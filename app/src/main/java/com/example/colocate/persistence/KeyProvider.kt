/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.persistence

interface KeyProvider {

    fun getKey(): ByteArray
}