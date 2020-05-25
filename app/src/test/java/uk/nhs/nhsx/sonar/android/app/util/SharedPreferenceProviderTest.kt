/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class SharedPreferenceProviderTest {
    private val sharedPreferences = mockk<SharedPreferences>()
    private val context = mockk<Context>()

    private val stringProvider = SharedPreferenceStringProvider(
        context,
        "name",
        "key",
        "default"
    )

    private val objectProvider = SharedPreferenceSerializingProvider(
        context,
        "name",
        "key",
        { it.name },
        { Dummy(it ?: "") })

    @Before
    fun setup() {
        every { context.getSharedPreferences("name", Context.MODE_PRIVATE) } returns sharedPreferences
    }

    @Test
    fun `clears existing value`() {
        val editor = mockk<SharedPreferences.Editor>()
        every { editor.clear() } returns editor
        every { editor.apply() } returns Unit

        every { sharedPreferences.edit() } returns editor

        stringProvider.clear()

        verify {
            sharedPreferences.edit()
            editor.clear()
            editor.apply()
        }
    }

    @Test
    fun `checks for existing value`() {
        every { sharedPreferences.contains("key") } returns true

        assertThat(stringProvider.has()).isTrue()

        verify {
            sharedPreferences.contains("key")
        }
    }

    @Test
    fun `checks for non-existing value`() {
        every { sharedPreferences.contains("key") } returns false

        assertThat(stringProvider.has()).isFalse()

        verify {
            sharedPreferences.contains("key")
        }
    }

    @Test
    fun `gets existing string value`() {
        every { sharedPreferences.getString("key", "default") } returns "::the value::"

        assertThat(stringProvider.get()).isEqualTo("::the value::")
    }

    @Test
    fun `sets new string value`() {
        val editor = mockk<SharedPreferences.Editor>()
        every { editor.putString("key", "::the new value::") } returns editor
        every { editor.apply() } returns Unit

        every { sharedPreferences.edit() } returns editor

        stringProvider.set("::the new value::")

        verify {
            sharedPreferences.edit()
            editor.putString("key", "::the new value::")
            editor.apply()
        }
    }

    @Test
    fun `gets existing object`() {
        every { sharedPreferences.getString("key", null) } returns "::the value::"

        assertThat(objectProvider.get()).isEqualTo(Dummy("::the value::"))
    }

    @Test
    fun `sets new object`() {
        val editor = mockk<SharedPreferences.Editor>()
        every { editor.putString("key", "::the new value::") } returns editor
        every { editor.apply() } returns Unit

        every { sharedPreferences.edit() } returns editor

        objectProvider.set(Dummy("::the new value::"))

        verify {
            sharedPreferences.edit()
            editor.putString("key", "::the new value::")
            editor.apply()
        }
    }

    data class Dummy(val name: String)
}
