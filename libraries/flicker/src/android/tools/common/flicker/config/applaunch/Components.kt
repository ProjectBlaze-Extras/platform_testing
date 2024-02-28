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

package android.tools.common.flicker.config.applaunch

import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.assertors.ComponentTemplate
import android.tools.common.traces.component.FullComponentIdMatcher
import android.tools.common.traces.component.IComponentMatcher
import android.tools.common.traces.wm.Transition
import android.tools.common.traces.wm.TransitionType

object Components {
    val OPENING_APP =
        ComponentTemplate("OPENING_APP") { scenarioInstance: ScenarioInstance ->
            Components.openingAppFrom(
                scenarioInstance.associatedTransition ?: error("Missing associated transition")
            )
        }

    private fun openingAppFrom(transition: Transition): IComponentMatcher {
        val targetChanges =
            transition.changes.filter {
                it.transitMode == TransitionType.OPEN || it.transitMode == TransitionType.TO_FRONT
            }

        val openingLayerIds = targetChanges.map { it.layerId }
        require(openingLayerIds.size == 1) {
            "Expected 1 opening layer but got ${openingLayerIds.size}"
        }

        val openingWindowIds = targetChanges.map { it.windowId }
        require(openingWindowIds.size == 1) {
            "Expected 1 opening window but got ${openingWindowIds.size}"
        }

        val windowId = openingWindowIds.first()
        val layerId = openingLayerIds.first()
        return FullComponentIdMatcher(windowId, layerId)
    }
}
