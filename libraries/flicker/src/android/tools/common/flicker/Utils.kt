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

package android.tools.common.flicker

import android.tools.common.traces.surfaceflinger.LayersTrace
import android.tools.common.traces.wm.TransitionChange
import android.tools.common.traces.wm.WindowManagerTrace

fun String.camelToSnakeCase(): String {
    return this.fold(StringBuilder()) { acc, c ->
            acc.let {
                val lowerC = c.lowercase()
                acc.append(if (acc.isNotEmpty() && c.isUpperCase()) "_$lowerC" else lowerC)
            }
        }
        .toString()
}

fun isAppTransitionChange(
    transitionChange: TransitionChange,
    layersTrace: LayersTrace?,
    wmTrace: WindowManagerTrace?
): Boolean {
    require(layersTrace != null || wmTrace != null) {
        "Requires at least one of wm of layers trace to not be null"
    }

    val layerDescriptors =
        layersTrace?.let {
            it.getLayerDescriptorById(transitionChange.layerId)
                ?: error("Failed to find layer with id ${transitionChange.layerId}")
        }
    val windowDescriptor =
        wmTrace?.let {
            it.getWindowDescriptorById(transitionChange.windowId)
                ?: error("Failed to find layer with id ${transitionChange.windowId}")
        }
    return (layerDescriptors?.isAppLayer ?: true) && (windowDescriptor?.isAppWindow ?: true)
}
