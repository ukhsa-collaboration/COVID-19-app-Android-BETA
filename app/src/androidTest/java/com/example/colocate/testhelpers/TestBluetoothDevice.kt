package com.example.colocate.testhelpers

import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState.CONNECTED
import com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState.CONNECTING
import com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState.DISCONNECTED
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.polidea.rxandroidble2.Timeout
import com.polidea.rxandroidble2.exceptions.BleAlreadyConnectedException
import com.polidea.rxandroidble2.exceptions.BleException
import com.polidea.rxandroidble2.mockrxandroidble.RxBleConnectionMock
import com.polidea.rxandroidble2.mockrxandroidble.RxBleDeviceMock
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.atomic.AtomicBoolean

class TestBluetoothDevice(macAddress: String, services: RxBleDeviceServices, rssiList: List<Int>) :
    RxBleDeviceMock("Fake device", macAddress, ByteArray(0), -1, services, mutableMapOf(), null) {

    private val isConnected = AtomicBoolean(false)
    private val connection =
        TestBluetoothConnection(services, rssiList)
    private val connectionStateBehaviorSubject = BehaviorSubject.createDefault(DISCONNECTED)

    override fun establishConnection(autoConnect: Boolean): Observable<RxBleConnection> {
        return Observable.defer {
            if (isConnected.compareAndSet(false, true)) {
                emitConnectionWithoutCompleting()
                    .doOnSubscribe {
                        connectionStateBehaviorSubject.onNext(CONNECTING)
                    }
                    .doOnNext {
                        connectionStateBehaviorSubject.onNext(CONNECTED)
                    }
                    .doFinally {
                        connectionStateBehaviorSubject.onNext(DISCONNECTED)
                        isConnected.set(false)
                    }
            } else {
                Observable.error(BleAlreadyConnectedException(macAddress))
            }
        }
    }

    override fun getConnectionState(): RxBleConnection.RxBleConnectionState =
        observeConnectionStateChanges().blockingFirst()

    override fun observeConnectionStateChanges(): Observable<RxBleConnection.RxBleConnectionState> =
        connectionStateBehaviorSubject.distinctUntilChanged()

    override fun establishConnection(autoConnect: Boolean, operationTimeout: Timeout): Observable<RxBleConnection> {
        return establishConnection(autoConnect)
    }

    private fun emitConnectionWithoutCompleting(): Observable<RxBleConnection> {
        return Observable.never<RxBleConnection>().startWith(connection)
    }
}

class TestBluetoothConnection(services: RxBleDeviceServices, private val rssiList: List<Int>) :
    RxBleConnectionMock(services, -1, mutableMapOf()) {

    var cursor = 0

    override fun readRssi(): Single<Int> {
        if (cursor < rssiList.size) {
            val single = Single.just(rssiList[cursor])
            cursor += 1
            return single
        }

        return Single.error(BleException("Test connection ran out of RSSI"))
    }
}
