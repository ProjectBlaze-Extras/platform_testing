/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.tools.common.traces.wm

import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
class WindowManagerTraceEntryBuilder {
    private var policy: WindowManagerPolicy? = null
    private var focusedApp = ""
    private var focusedDisplayId = 0
    private var focusedWindow = ""
    private var inputMethodWindowAppToken = ""
    private var isHomeRecentsComponent = false
    private var isDisplayFrozen = false
    private val pendingActivities = mutableListOf<String>()
    private var root: RootWindowContainer? = null
    private var keyguardControllerState: KeyguardControllerState? = null
    private var where = ""

    // Necessary for compatibility with JS number type
    private var elapsedTimestamp: Long = 0L
    private var realTimestamp: Long? = null

    @JsName("setPolicy")
    fun setPolicy(value: WindowManagerPolicy?): WindowManagerTraceEntryBuilder = apply {
        policy = value
    }

    @JsName("setFocusedApp")
    fun setFocusedApp(value: String): WindowManagerTraceEntryBuilder = apply { focusedApp = value }

    @JsName("setFocusedDisplayId")
    fun setFocusedDisplayId(value: Int): WindowManagerTraceEntryBuilder = apply {
        focusedDisplayId = value
    }

    @JsName("setFocusedWindow")
    fun setFocusedWindow(value: String): WindowManagerTraceEntryBuilder = apply {
        focusedWindow = value
    }

    @JsName("setInputMethodWindowAppToken")
    fun setInputMethodWindowAppToken(value: String): WindowManagerTraceEntryBuilder = apply {
        inputMethodWindowAppToken = value
    }

    @JsName("setIsHomeRecentsComponent")
    fun setIsHomeRecentsComponent(value: Boolean): WindowManagerTraceEntryBuilder = apply {
        isHomeRecentsComponent = value
    }

    @JsName("setIsDisplayFrozen")
    fun setIsDisplayFrozen(value: Boolean): WindowManagerTraceEntryBuilder = apply {
        isDisplayFrozen = value
    }

    @JsName("setPendingActivities")
    fun setPendingActivities(value: Array<String>): WindowManagerTraceEntryBuilder = apply {
        pendingActivities.addAll(value)
    }

    @JsName("setRoot")
    fun setRoot(value: RootWindowContainer?): WindowManagerTraceEntryBuilder = apply {
        root = value
    }

    @JsName("setKeyguardControllerState")
    fun setKeyguardControllerState(
        value: KeyguardControllerState?
    ): WindowManagerTraceEntryBuilder = apply { keyguardControllerState = value }

    @JsName("setWhere")
    fun setWhere(value: String): WindowManagerTraceEntryBuilder = apply { where = value }

    @JsName("setElapsedTimestamp")
    fun setElapsedTimestamp(value: String): WindowManagerTraceEntryBuilder =
        // Necessary for compatibility with JS number type
        apply { elapsedTimestamp = value.toLong() }

    @JsName("setRealToElapsedTimeOffsetNs")
    fun setRealToElapsedTimeOffsetNs(value: String?): WindowManagerTraceEntryBuilder = apply {
        realTimestamp =
            if (value != null && value.toLong() != 0L) {
                value.toLong() + elapsedTimestamp
            } else {
                null
            }
    }

    /** Constructs the window manager trace entry. */
    @JsName("build")
    fun build(): WindowManagerState {
        val root = root ?: error("Root not set")
        val keyguardControllerState =
            keyguardControllerState ?: error("KeyguardControllerState not set")

        return WindowManagerState(
            elapsedTimestamp,
            realTimestamp,
            where,
            policy,
            focusedApp,
            focusedDisplayId,
            focusedWindow,
            inputMethodWindowAppToken,
            isHomeRecentsComponent,
            isDisplayFrozen,
            pendingActivities.toTypedArray(),
            root,
            keyguardControllerState
        )
    }
}
