/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di

import com.polidea.rxandroidble2.RxBleClient
import dagger.Component
import uk.nhs.nhsx.sonar.android.app.BootCompletedReceiver
import uk.nhs.nhsx.sonar.android.app.FlowTestStartActivity
import uk.nhs.nhsx.sonar.android.app.MainActivity
import uk.nhs.nhsx.sonar.android.app.ble.BleEvents
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventV2Dao
import uk.nhs.nhsx.sonar.android.app.contactevents.DeleteOutdatedEvents
import uk.nhs.nhsx.sonar.android.app.di.module.AppModule
import uk.nhs.nhsx.sonar.android.app.di.module.BluetoothModule
import uk.nhs.nhsx.sonar.android.app.di.module.CryptoModule
import uk.nhs.nhsx.sonar.android.app.di.module.NetworkModule
import uk.nhs.nhsx.sonar.android.app.di.module.NotificationsModule
import uk.nhs.nhsx.sonar.android.app.di.module.PersistenceModule
import uk.nhs.nhsx.sonar.android.app.di.module.RegistrationModule
import uk.nhs.nhsx.sonar.android.app.di.module.StatusModule
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseReviewActivity
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationService
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeActivity
import uk.nhs.nhsx.sonar.android.app.registration.RegistrationUseCase
import uk.nhs.nhsx.sonar.android.app.status.AtRiskActivity
import uk.nhs.nhsx.sonar.android.app.status.IsolateActivity
import uk.nhs.nhsx.sonar.android.app.status.OkActivity
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        PersistenceModule::class,
        AppModule::class,
        BluetoothModule::class,
        CryptoModule::class,
        NetworkModule::class,
        EncryptionKeyStorageModule::class,
        StatusModule::class,
        RegistrationModule::class,
        NotificationsModule::class
    ]
)
interface ApplicationComponent {
    fun inject(deleteOutdatedEvents: DeleteOutdatedEvents)
    fun inject(bluetoothService: BluetoothService)
    fun inject(isolateActivity: IsolateActivity)
    fun inject(okActivity: OkActivity)
    fun inject(atRiskActivity: AtRiskActivity)
    fun inject(diagnoseReviewActivity: DiagnoseReviewActivity)
    fun inject(mainActivity: MainActivity)
    fun inject(notificationService: NotificationService)
    fun inject(flowTestStartActivity: FlowTestStartActivity)
    fun inject(bootCompletedReceiver: BootCompletedReceiver)
    fun inject(postCodeActivity: PostCodeActivity)

    fun registrationUseCase(): RegistrationUseCase
    fun provideEventsV2Dao(): ContactEventV2Dao
    fun provideBleEvents(): BleEvents
    fun provideRxBleClient(): RxBleClient
}
