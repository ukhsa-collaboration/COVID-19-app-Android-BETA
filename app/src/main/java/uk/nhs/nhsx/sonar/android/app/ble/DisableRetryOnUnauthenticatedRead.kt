/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.ble

import android.bluetooth.BluetoothGatt
import timber.log.Timber
import java.lang.reflect.Field

/*
* When BluetoothGatt.readCharacteristic is called on a characteristic that requires bonding of some
* sort (i.e. Authenticated/Authorised/Encrypted), BluetoothGatt.mBluetoothGattCallback will
* automatically attempt to bond with the remote device (see
* https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/bluetooth/BluetoothGatt.java).
*
* Specifically, BluetoothGatt.mBluetoothGattCallback.onCharacteristicRead and
* BluetoothGatt.mBluetoothGattCallback.onCharacteristicWrite will be called by the operating system
* (see https://android.googlesource.com/platform/system/bt/) and, should the response status  be
* GATT_INSUFFICIENT_AUTHENTICATION or GATT_INSUFFICIENT_ENCRYPTION it will attempt to re-read/write
* the characteristic requesting either AUTHENTICATION_MITM or AUTHENTICATION_NO_MITM and increment
* the retry counter BluetoothGatt.mAuthRetryState. Should the counter already be equal to
* AUTH_RETRY_STATE_MITM the read will fail.
*
* Should the read succeed, mAuthRetryState will be reset to AUTH_RETRY_STATE_IDLE (zero)
*
* In order to prevent this unnecessary bonding, this object
* provides the function bypassAuthenticationRetry which will set mAuthRetryState to
* AUTH_RETRY_STATE_MITM on the provided BluetoothGatt such that, should authentication be required
* for the next GATT operation the device will not offer it.
*
* The class also supports a similar bypass against the older "mAuthState" flag.
*
* This leverages Java's reflection API.
*/
object DisableRetryOnUnauthenticatedRead {
    private var initFailed = false
    private var initComplete = false

    private var bluetoothGattClass = BluetoothGatt::class.java

    // We can't read the current value of AUTH_RETRY_STATE_MITM, it's in the dark grey list for 28+.
    // https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/bluetooth/BluetoothGatt.java#70
    private const val AUTH_RETRY_STATE_MITM: Int = 2
    private var mAuthRetryStateField: Field? = null
    private var mAuthRetryField: Field? = null

    /**
     * Initialises all the reflection references used by bypassAuthenticationRetry
     * This has been checked against the source of Android 10_r36
     * Returns true if object is in valid state
     */
    @Synchronized
    private fun tryInit(): Boolean {
        // Check if function has already run and failed
        if (initFailed || initComplete) {
            return !initFailed
        }

        try {
            try {
                // Get a reference to the mAuthRetryState
                // This will throw NoSuchFieldException on older android, which is handled below
                mAuthRetryStateField = bluetoothGattClass.getDeclaredField("mAuthRetryState")
            } catch (e: NoSuchFieldException) {
                // Prior to https://android.googlesource.com/platform/frameworks/base/+/3854e2267487ecd129bdd0711c6d9dfbf8f7ed0d%5E%21/#F0,
                // And at least after Marshmallow (6), mAuthRetryField (a boolean) was used instead
                // of mAuthRetryState
                mAuthRetryField = bluetoothGattClass.getDeclaredField("mAuthRetry")
            }

            // Should be good to go now
            initComplete = true
            initFailed = false
            return true
        } catch (e: NoSuchFieldException) {
            Timber.d("Unable to find field while initialising: ${e.message}")
        } catch (e: SecurityException) {
            Timber.d("Encountered sandbox exception while initialising: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Timber.d("Encountered argument exception while initialising: ${e.message}")
        } catch (e: NullPointerException) {
            Timber.d("Encountered NPE while initialising: ${e.message}")
        } catch (e: ExceptionInInitializerError) {
            Timber.d("Encountered reflection initialisation error: ${e.message}")
        }

        Timber.d("Failed to initialise, bypassAuthenticationRetry will quietly fail")
        initComplete = true
        initFailed = true
        return false
    }

    /**
     * This function will attempt to bypass the conditionals in BluetoothGatt.mBluetoothGattCallback
     * that cause bonding to occur.
     *
     * The function will fail silently if any errors occur during initialisation or patching.
     *
     * See
     * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/bluetooth/BluetoothGatt.java#367
     * for an example of the conditional that is bypassed
     */
    @Synchronized
    fun bypassAuthenticationRetry(gatt: BluetoothGatt) {
        if (!tryInit()) {
            // Class failed to initialised correctly, return quietly
            return
        }

        try {
            if (mAuthRetryStateField != null) {
                Timber.d("Attempting to bypass mAuthRetryState bonding conditional")
                val mAuthRetryStateAccessible = mAuthRetryStateField!!.isAccessible
                if (!mAuthRetryStateAccessible) {
                    mAuthRetryStateField!!.isAccessible = true
                }

                // The conditional branch that causes binding to occur in BluetoothGatt do not occur if
                // mAuthRetryState == AUTH_RETRY_STATE_MITM, as this signifies that both steps of
                // authenticated/encrypted reading have failed to establish.
                mAuthRetryStateField!!.setInt(gatt, AUTH_RETRY_STATE_MITM)

                mAuthRetryStateField!!.isAccessible = mAuthRetryStateAccessible
            } else {
                Timber.d("Attempting to bypass mAuthRetry bonding conditional")
                val mAuthRetryAccessible = mAuthRetryField!!.isAccessible
                if (!mAuthRetryAccessible) {
                    mAuthRetryField!!.isAccessible = true
                }

                // The conditional branch that causes binding to occur in BluetoothGatt do not occur if
                // mAuthRetry == true, as this signifies an attempt was made to bind
                mAuthRetryField!!.setBoolean(gatt, true)

                mAuthRetryField!!.isAccessible = mAuthRetryAccessible
            }
        } catch (e: SecurityException) {
            Timber.d("Encountered sandbox exception in bypassAuthenticationRetry: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Timber.d("Encountered argument exception in bypassAuthenticationRetry: ${e.message}")
        } catch (e: NullPointerException) {
            Timber.d("Encountered NPE in bypassAuthenticationRetry: ${e.message}")
        } catch (e: ExceptionInInitializerError) {
            Timber.d("Encountered reflection in bypassAuthenticationRetry: ${e.message}")
        }
    }
}
