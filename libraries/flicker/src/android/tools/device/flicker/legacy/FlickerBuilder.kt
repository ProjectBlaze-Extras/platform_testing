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

package android.tools.device.flicker.legacy

import android.app.Instrumentation
import android.tools.common.io.TraceType
import android.tools.common.traces.wm.TransitionsTrace
import android.tools.common.traces.wm.WindowManagerState
import android.tools.common.traces.wm.WindowManagerTrace
import android.tools.device.traces.getDefaultFlickerOutputDir
import android.tools.device.traces.monitors.ITransitionMonitor
import android.tools.device.traces.monitors.NoTraceMonitor
import android.tools.device.traces.monitors.PerfettoTraceMonitor
import android.tools.device.traces.monitors.ScreenRecorder
import android.tools.device.traces.monitors.events.EventLogMonitor
import android.tools.device.traces.monitors.view.ViewTraceMonitor
import android.tools.device.traces.monitors.wm.ShellTransitionTraceMonitor
import android.tools.device.traces.monitors.wm.WindowManagerTraceMonitor
import android.tools.device.traces.monitors.wm.WmTransitionTraceMonitor
import android.tools.device.traces.parsers.WindowManagerStateHelper
import androidx.test.uiautomator.UiDevice
import java.io.File

/** Build Flicker tests using Flicker DSL */
@FlickerDslMarker
class FlickerBuilder(
    private val instrumentation: Instrumentation,
    private val outputDir: File = getDefaultFlickerOutputDir(),
    private val wmHelper: WindowManagerStateHelper =
        WindowManagerStateHelper(instrumentation, clearCacheAfterParsing = false),
    private val setupCommands: MutableList<FlickerTestData.() -> Any> = mutableListOf(),
    private val transitionCommands: MutableList<FlickerTestData.() -> Any> = mutableListOf(),
    private val teardownCommands: MutableList<FlickerTestData.() -> Any> = mutableListOf(),
    val device: UiDevice = UiDevice.getInstance(instrumentation),
    private val traceMonitors: MutableList<ITransitionMonitor> =
        mutableListOf<ITransitionMonitor>().also {
            it.add(WindowManagerTraceMonitor())
            it.add(PerfettoTraceMonitor().enableLayersTrace().enableTransactionsTrace())
            it.add(WmTransitionTraceMonitor())
            it.add(ShellTransitionTraceMonitor())
            it.add(ScreenRecorder(instrumentation.targetContext))
            it.add(EventLogMonitor())
            it.add(ViewTraceMonitor())
        }
) {
    private var usingExistingTraces = false

    /**
     * Configure a [WindowManagerTraceMonitor] to obtain [WindowManagerTrace]
     *
     * By default, the tracing is always active. To disable tracing return null
     *
     * If this tracing is disabled, the assertions for [WindowManagerTrace] and [WindowManagerState]
     * will not be executed
     */
    fun withWindowManagerTracing(traceMonitor: () -> WindowManagerTraceMonitor?): FlickerBuilder =
        apply {
            traceMonitors.removeIf { it is WindowManagerTraceMonitor }
            addMonitor(traceMonitor())
        }

    /**
     * Configure a [WmTransitionTraceMonitor] to obtain [TransitionsTrace].
     *
     * By default, shell transition tracing is disabled.
     */
    fun withTransitionTracing(traceMonitor: () -> WmTransitionTraceMonitor?): FlickerBuilder =
        apply {
            traceMonitors.removeIf { it is WmTransitionTraceMonitor }
            addMonitor(traceMonitor())
        }

    /**
     * Configure a [ScreenRecorder].
     *
     * By default, the tracing is always active. To disable tracing return null
     */
    fun withScreenRecorder(screenRecorder: () -> ScreenRecorder?): FlickerBuilder = apply {
        traceMonitors.removeIf { it is ScreenRecorder }
        addMonitor(screenRecorder())
    }

    fun withoutScreenRecorder(): FlickerBuilder = apply {
        traceMonitors.removeIf { it is ScreenRecorder }
    }

    /** Defines the setup commands executed before the [transitions] to test */
    fun setup(commands: FlickerTestData.() -> Unit): FlickerBuilder = apply {
        setupCommands.add(commands)
    }

    /** Defines the teardown commands executed after the [transitions] to test */
    fun teardown(commands: FlickerTestData.() -> Unit): FlickerBuilder = apply {
        teardownCommands.add(commands)
    }

    /** Defines the commands that trigger the behavior to test */
    fun transitions(command: FlickerTestData.() -> Unit): FlickerBuilder = apply {
        require(!usingExistingTraces) {
            "Can't update transition after calling usingExistingTraces"
        }
        transitionCommands.add(command)
    }

    data class TraceFiles(
        val wmTrace: File,
        val perfetto: File,
        val wmTransitions: File,
        val shellTransitions: File,
        val eventLog: File
    )

    /** Use pre-executed results instead of running transitions to get the traces */
    fun usingExistingTraces(_traceFiles: () -> TraceFiles): FlickerBuilder = apply {
        val traceFiles = _traceFiles()
        // Remove all trace monitor and use only monitor that read from existing trace file
        this.traceMonitors.clear()
        addMonitor(NoTraceMonitor { it.addTraceResult(TraceType.WM, traceFiles.wmTrace) })
        addMonitor(NoTraceMonitor { it.addTraceResult(TraceType.SF, traceFiles.perfetto) })
        addMonitor(NoTraceMonitor { it.addTraceResult(TraceType.TRANSACTION, traceFiles.perfetto) })
        addMonitor(
            NoTraceMonitor { it.addTraceResult(TraceType.WM_TRANSITION, traceFiles.wmTransitions) }
        )
        addMonitor(
            NoTraceMonitor {
                it.addTraceResult(TraceType.SHELL_TRANSITION, traceFiles.shellTransitions)
            }
        )
        addMonitor(NoTraceMonitor { it.addTraceResult(TraceType.EVENT_LOG, traceFiles.eventLog) })

        // Remove all transitions execution
        this.transitionCommands.clear()
        this.usingExistingTraces = true
    }

    /** Creates a new Flicker runner based on the current builder configuration */
    fun build(): FlickerTestData {
        return FlickerTestDataImpl(
            instrumentation,
            device,
            outputDir,
            traceMonitors,
            setupCommands,
            transitionCommands,
            teardownCommands,
            wmHelper
        )
    }

    /** Returns a copy of the current builder with the changes of [block] applied */
    fun copy(block: FlickerBuilder.() -> Unit) =
        FlickerBuilder(
                instrumentation,
                outputDir.absoluteFile,
                wmHelper,
                setupCommands.toMutableList(),
                transitionCommands.toMutableList(),
                teardownCommands.toMutableList(),
                device,
                traceMonitors.toMutableList(),
            )
            .apply(block)

    private fun addMonitor(newMonitor: ITransitionMonitor?) {
        require(!usingExistingTraces) { "Can't add monitors after calling usingExistingTraces" }

        if (newMonitor != null) {
            traceMonitors.add(newMonitor)
        }
    }
}
