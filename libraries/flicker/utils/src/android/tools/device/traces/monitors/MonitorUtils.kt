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

@file:JvmName("MonitorUtils")
@file:OptIn(
    androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi::class,
    androidx.benchmark.perfetto.ExperimentalPerfettoTraceProcessorApi::class
)

package android.tools.device.traces.monitors

import android.tools.common.ScenarioBuilder
import android.tools.common.Tag
import android.tools.common.io.Reader
import android.tools.common.traces.surfaceflinger.LayersTrace
import android.tools.common.traces.wm.WindowManagerTrace
import android.tools.device.traces.SERVICE_TRACE_CONFIG
import android.tools.device.traces.io.ResultReaderWithLru
import android.tools.device.traces.io.ResultWriter
import android.tools.device.traces.monitors.wm.WindowManagerTraceMonitor
import android.tools.device.traces.parsers.perfetto.LayersTraceParser
import android.tools.device.traces.parsers.perfetto.TraceProcessorSession
import java.io.File
import perfetto.protos.PerfettoConfig.SurfaceFlingerLayersConfig

/**
 * Acquire the [LayersTrace] with the device state changes that happen when executing the commands
 * defined in the [predicate].
 *
 * @param flags Flags to indicate tracing level
 * @param predicate Commands to execute
 * @throws UnsupportedOperationException If tracing is already activated
 */
@JvmOverloads
fun withSFTracing(
    flags: List<SurfaceFlingerLayersConfig.TraceFlag>? = null,
    predicate: () -> Unit
): LayersTrace {
    val trace = PerfettoTraceMonitor().enableLayersTrace(flags).withTracing(Tag.ALL, predicate)
    return TraceProcessorSession.loadPerfettoTrace(trace) { session ->
        LayersTraceParser().parse(session)
    }
}

/**
 * Acquire the [WindowManagerTrace] and [LayersTrace] with the device state changes that happen when
 * executing the commands defined in the [predicate].
 *
 * @param predicate Commands to execute
 * @throws UnsupportedOperationException If tracing is already activated
 */
fun withTracing(
    traceMonitors: List<TraceMonitor> =
        listOf(
            WindowManagerTraceMonitor(),
            PerfettoTraceMonitor().enableLayersTrace().enableTransactionsTrace(),
        ),
    predicate: () -> Unit
): Reader {
    val tmpFile = File.createTempFile("recordTraces", "")
    val writer =
        ResultWriter()
            .forScenario(ScenarioBuilder().forClass(tmpFile.name).build())
            .withOutputDir(tmpFile.parentFile)

    try {
        traceMonitors.forEach { it.start() }
        predicate()
    } finally {
        traceMonitors.forEach { it.stop(writer) }
    }
    return ResultReaderWithLru(writer.write(), SERVICE_TRACE_CONFIG)
}
