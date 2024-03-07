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

package android.tools.device.traces.monitors

import android.tools.common.io.TraceType
import android.tools.device.traces.executeShellCommand
import android.tools.device.traces.io.IoUtils
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.locks.ReentrantLock
import perfetto.protos.PerfettoConfig.DataSourceConfig
import perfetto.protos.PerfettoConfig.SurfaceFlingerLayersConfig
import perfetto.protos.PerfettoConfig.SurfaceFlingerTransactionsConfig
import perfetto.protos.PerfettoConfig.TraceConfig

/* Captures traces from Perfetto. */
open class PerfettoTraceMonitor : TraceMonitor() {
    override val traceType = TraceType.SF // TODO: is this ok for the time being?
    override val isEnabled
        get() = perfettoPid != null

    private val DEFAULT_SF_LAYER_FLAGS =
        listOf(
            SurfaceFlingerLayersConfig.TraceFlag.TRACE_FLAG_INPUT,
            SurfaceFlingerLayersConfig.TraceFlag.TRACE_FLAG_COMPOSITION,
            SurfaceFlingerLayersConfig.TraceFlag.TRACE_FLAG_VIRTUAL_DISPLAYS
        )

    private var isLayersTraceEnabled = false
    private var layersTraceFlags = DEFAULT_SF_LAYER_FLAGS

    private var isLayersDumpEnabled = false
    private var layersDumpFlags = DEFAULT_SF_LAYER_FLAGS

    private var isTransactionsTraceEnabled = false

    private var perfettoPid: Int? = null
    private var configFileInPerfettoDir: File? = null
    private var traceFile: File? = null
    private var traceFileInPerfettoDir: File? = null
    private val PERFETTO_CONFIGS_DIR = File("/data/misc/perfetto-configs")
    private val PERFETTO_TRACES_DIR = File("/data/misc/perfetto-traces")

    fun enableLayersTrace(
        flags: List<SurfaceFlingerLayersConfig.TraceFlag>? = null
    ): PerfettoTraceMonitor {
        isLayersTraceEnabled = true
        if (flags != null) {
            layersTraceFlags = flags
        }
        return this
    }

    fun enableLayersDump(
        flags: List<SurfaceFlingerLayersConfig.TraceFlag>? = null
    ): PerfettoTraceMonitor {
        isLayersDumpEnabled = true
        if (flags != null) {
            layersDumpFlags = flags
        }
        return this
    }

    fun enableTransactionsTrace(): PerfettoTraceMonitor {
        isTransactionsTraceEnabled = true
        return this
    }

    override fun doStart() {
        val configFile = File.createTempFile("flickerlib-config-", ".cfg")
        configFileInPerfettoDir = PERFETTO_CONFIGS_DIR.resolve(requireNotNull(configFile).name)

        traceFile = File.createTempFile(traceType.fileName, "")
        traceFileInPerfettoDir = PERFETTO_TRACES_DIR.resolve(requireNotNull(traceFile).name)

        val configBuilder =
            TraceConfig.newBuilder()
                .setDurationMs(0)
                .addBuffers(
                    TraceConfig.BufferConfig.newBuilder().setSizeKb(TRACE_BUFFER_SIZE_KB).build()
                )

        if (isLayersTraceEnabled) {
            configBuilder.addDataSources(createLayersTraceDataSourceConfig())
        }

        if (isLayersDumpEnabled) {
            configBuilder.addDataSources(createLayersDumpDataSourceConfig())
        }

        if (isTransactionsTraceEnabled) {
            configBuilder.addDataSources(createTransactionsDataSourceConfig())
        }

        val config = configBuilder.build()

        FileOutputStream(configFile).use { config.writeTo(it) }
        IoUtils.moveFile(requireNotNull(configFile), requireNotNull(configFileInPerfettoDir))

        val command =
            "perfetto --background-wait" +
                " --config ${configFileInPerfettoDir?.absolutePath}" +
                " --out ${traceFileInPerfettoDir?.absolutePath}"
        val stdout = String(executeShellCommand(command))
        val pid = stdout.trim().toInt()
        perfettoPid = pid
        allPerfettoPidsLock.lock()
        try {
            allPerfettoPids.add(pid)
        } finally {
            allPerfettoPidsLock.unlock()
        }
    }

    override fun doStop(): File {
        require(isEnabled) { "Attempted to stop disabled trace monitor" }
        killPerfettoProcess(requireNotNull(perfettoPid))
        waitPerfettoProcessExits(requireNotNull(perfettoPid))
        IoUtils.moveFile(requireNotNull(traceFileInPerfettoDir), requireNotNull(traceFile))
        executeShellCommand("rm ${configFileInPerfettoDir?.absolutePath}")
        perfettoPid = null
        return requireNotNull(traceFile)
    }

    private fun createLayersTraceDataSourceConfig(): TraceConfig.DataSource {
        return TraceConfig.DataSource.newBuilder()
            .setConfig(
                DataSourceConfig.newBuilder()
                    .setName(SF_LAYERS_DATA_SOURCE)
                    .setSurfaceflingerLayersConfig(
                        SurfaceFlingerLayersConfig.newBuilder()
                            .setMode(SurfaceFlingerLayersConfig.Mode.MODE_ACTIVE)
                            .apply { layersTraceFlags.forEach { addTraceFlags(it) } }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun createLayersDumpDataSourceConfig(): TraceConfig.DataSource {
        return TraceConfig.DataSource.newBuilder()
            .setConfig(
                DataSourceConfig.newBuilder()
                    .setName(SF_LAYERS_DATA_SOURCE)
                    .setSurfaceflingerLayersConfig(
                        SurfaceFlingerLayersConfig.newBuilder()
                            .setMode(SurfaceFlingerLayersConfig.Mode.MODE_DUMP)
                            .apply { layersDumpFlags.forEach { addTraceFlags(it) } }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun createTransactionsDataSourceConfig(): TraceConfig.DataSource {
        return TraceConfig.DataSource.newBuilder()
            .setConfig(
                DataSourceConfig.newBuilder()
                    .setName(SF_TRANSACTIONS_DATA_SOURCE)
                    .setSurfaceflingerTransactionsConfig(
                        SurfaceFlingerTransactionsConfig.newBuilder()
                            .setMode(SurfaceFlingerTransactionsConfig.Mode.MODE_ACTIVE)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    companion object {
        private const val TRACE_BUFFER_SIZE_KB = 1024 * 1024

        private const val SF_LAYERS_DATA_SOURCE = "android.surfaceflinger.layers"
        private const val SF_TRANSACTIONS_DATA_SOURCE = "android.surfaceflinger.transactions"

        private val allPerfettoPids = mutableListOf<Int>()
        private val allPerfettoPidsLock = ReentrantLock()

        fun stopAllSessions() {
            allPerfettoPidsLock.lock()
            try {
                allPerfettoPids.forEach { killPerfettoProcess(it) }
                allPerfettoPids.forEach { waitPerfettoProcessExits(it) }
            } finally {
                allPerfettoPidsLock.unlock()
            }
        }

        fun killPerfettoProcess(pid: Int) {
            if (isPerfettoProcessUp(pid)) {
                executeShellCommand("kill $pid")
            }
        }

        private fun waitPerfettoProcessExits(pid: Int) {
            while (true) {
                if (!isPerfettoProcessUp(pid)) {
                    break
                }
                Thread.sleep(50)
            }
        }

        private fun isPerfettoProcessUp(pid: Int): Boolean {
            val out = String(executeShellCommand("ps -p $pid -o CMD"))
            return out.contains("perfetto")
        }
    }
}
