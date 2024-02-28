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

package android.tools.common.flicker.config.appclose

import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.assertors.ComponentTemplate
import android.tools.common.traces.component.FullComponentIdMatcher
import android.tools.common.traces.component.IComponentMatcher
import android.tools.common.traces.wm.Transition
import android.tools.common.traces.wm.TransitionType

object Components {
    val CLOSING_APP =
        ComponentTemplate("CLOSING_APP") { scenarioInstance: ScenarioInstance ->
            closingAppFrom(
                scenarioInstance.associatedTransition ?: error("Missing associated transition")
            )
        }

    private fun closingAppFrom(transition: Transition): IComponentMatcher {
        val targetChanges =
            transition.changes.filter {
                it.transitMode == TransitionType.CLOSE || it.transitMode == TransitionType.TO_BACK
            }

        val closingLayerIds = targetChanges.map { it.layerId }
        require(closingLayerIds.size == 1) {
            "Expected 1 closing layer but got ${closingLayerIds.size}"
        }

        val closingWindowIds = targetChanges.map { it.windowId }
        require(closingWindowIds.size == 1) {
            "Expected 1 closing window but got ${closingWindowIds.size}"
        }

        val windowId = closingWindowIds.first()
        val layerId = closingLayerIds.first()
        return FullComponentIdMatcher(windowId, layerId)
    }
}
