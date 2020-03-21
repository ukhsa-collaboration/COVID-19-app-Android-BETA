/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

class ContactEventsRecorder {
    private var contactEvents: MutableList<ContactEvent> = mutableListOf()

    fun appendEvent(contactEvent: ContactEvent) {
        contactEvents.add(contactEvent)
    }
}