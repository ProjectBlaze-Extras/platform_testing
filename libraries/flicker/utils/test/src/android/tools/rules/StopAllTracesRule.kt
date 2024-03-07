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

package android.tools.rules

import android.tools.device.traces.monitors.PerfettoTraceMonitor
import android.tools.device.traces.monitors.TraceMonitor
import android.tools.device.traces.monitors.view.ViewTraceMonitor
import android.tools.device.traces.monitors.wm.ShellTransitionTraceMonitor
import android.tools.device.traces.monitors.wm.WindowManagerTraceMonitor
import android.tools.device.traces.monitors.wm.WmTransitionTraceMonitor
import android.tools.utils.newTestResultWriter
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class StopAllTracesRule : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                PerfettoTraceMonitor.stopAllSessions()
                ShellTransitionTraceMonitor().stopIfEnabled()
                WindowManagerTraceMonitor().stopIfEnabled()
                WmTransitionTraceMonitor().stopIfEnabled()
                ViewTraceMonitor().stopIfEnabled()

                base?.evaluate()
            }
        }
    }

    companion object {
        private fun TraceMonitor.stopIfEnabled() {
            if (isEnabled) stop(newTestResultWriter())
        }
    }
}
