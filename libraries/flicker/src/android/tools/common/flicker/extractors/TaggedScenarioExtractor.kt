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

package android.tools.common.flicker.extractors

import android.tools.common.Timestamp
import android.tools.common.Timestamps
import android.tools.common.io.Reader
import android.tools.common.traces.events.Cuj
import android.tools.common.traces.events.CujType
import android.tools.common.traces.wm.Transition
import kotlin.math.max
import kotlin.math.min

class TaggedScenarioExtractor(
    private val targetTag: CujType,
    private val transitionMatcher: TransitionMatcher,
    private val adjustCuj: CujAdjust
) : ScenarioExtractor {
    override fun extract(reader: Reader): List<TraceSlice> {
        val cujTrace = reader.readCujTrace() ?: error("Missing CUJ trace")

        val targetCujEntries =
            cujTrace.entries
                .filter { it.cuj === targetTag }
                .filter { !it.canceled }
                .map { adjustCuj.adjustCuj(it, reader) }

        if (targetCujEntries.isEmpty()) {
            // No scenarios to extract here
            return emptyList()
        }

        return targetCujEntries.map { cujEntry ->
            val associatedTransition =
                transitionMatcher.getMatches(reader, cujEntry).firstOrNull()
                    ?: error("Missing associated transition")

            require(
                cujEntry.startTimestamp.hasAllTimestamps && cujEntry.endTimestamp.hasAllTimestamps
            )

            val startTimestamp =
                estimateScenarioStartTimestamp(cujEntry, associatedTransition, reader)
            val endTimestamp = estimateScenarioEndTimestamp(cujEntry, associatedTransition, reader)

            TraceSlice(
                startTimestamp,
                endTimestamp,
                associatedCuj = cujEntry.cuj,
                associatedTransition = associatedTransition
            )
        }
    }

    private fun estimateScenarioStartTimestamp(
        cujEntry: Cuj,
        associatedTransition: Transition?,
        reader: Reader
    ): Timestamp {
        val interpolatedStartTimestamp =
            if (associatedTransition != null) {
                Utils.interpolateStartTimestampFromTransition(associatedTransition, reader)
            } else {
                null
            }

        return Timestamps.from(
            elapsedNanos =
                min(
                    cujEntry.startTimestamp.elapsedNanos,
                    interpolatedStartTimestamp?.elapsedNanos ?: cujEntry.startTimestamp.elapsedNanos
                ),
            systemUptimeNanos =
                min(
                    cujEntry.startTimestamp.systemUptimeNanos,
                    interpolatedStartTimestamp?.systemUptimeNanos
                        ?: cujEntry.startTimestamp.systemUptimeNanos
                ),
            unixNanos =
                min(
                    cujEntry.startTimestamp.unixNanos,
                    interpolatedStartTimestamp?.unixNanos ?: cujEntry.startTimestamp.unixNanos
                )
        )
    }

    private fun estimateScenarioEndTimestamp(
        cujEntry: Cuj,
        associatedTransition: Transition?,
        reader: Reader
    ): Timestamp {
        val interpolatedEndTimestamp =
            if (associatedTransition != null) {
                Utils.interpolateFinishTimestampFromTransition(associatedTransition, reader)
            } else {
                null
            }

        return Timestamps.from(
            elapsedNanos =
                max(
                    cujEntry.endTimestamp.elapsedNanos,
                    interpolatedEndTimestamp?.elapsedNanos ?: -1L
                ),
            systemUptimeNanos =
                max(
                    cujEntry.endTimestamp.systemUptimeNanos,
                    interpolatedEndTimestamp?.systemUptimeNanos ?: -1L
                ),
            unixNanos =
                max(cujEntry.endTimestamp.unixNanos, interpolatedEndTimestamp?.unixNanos ?: -1L)
        )
    }
}
