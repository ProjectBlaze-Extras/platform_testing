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

package android.tools.device.flicker.datastore

import android.tools.common.Scenario
import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.assertions.ScenarioAssertion
import android.tools.device.traces.io.IResultData
import androidx.annotation.VisibleForTesting

/** In memory data store for flicker transitions, assertions and results */
object DataStore {
    private var cachedResults = mutableMapOf<Scenario, IResultData>()
    private var cachedFlickerServiceAssertions =
        mutableMapOf<Scenario, Map<ScenarioInstance, Collection<ScenarioAssertion>>>()

    data class Backup(
        val cachedResults: MutableMap<Scenario, IResultData>,
        val cachedFlickerServiceAssertions:
            MutableMap<Scenario, Map<ScenarioInstance, Collection<ScenarioAssertion>>>
    )

    @VisibleForTesting
    fun clear() {
        cachedResults = mutableMapOf()
        cachedFlickerServiceAssertions = mutableMapOf()
    }

    fun backup(): Backup {
        return Backup(cachedResults.toMutableMap(), cachedFlickerServiceAssertions.toMutableMap())
    }

    fun restore(backup: Backup) {
        cachedResults = backup.cachedResults
        cachedFlickerServiceAssertions = backup.cachedFlickerServiceAssertions
    }

    /** @return if the store has results for [scenario] */
    fun containsResult(scenario: Scenario): Boolean = cachedResults.containsKey(scenario)

    /**
     * Adds [result] to the store with [scenario] as id
     *
     * @throws IllegalStateException is [scenario] already exists in the data store
     */
    fun addResult(scenario: Scenario, result: IResultData) {
        require(!containsResult(scenario)) { "Result for $scenario already in data store" }
        cachedResults[scenario] = result
    }

    /**
     * Replaces the old value [scenario] result in the store by [newResult]
     *
     * @throws IllegalStateException is [scenario] doesn't exist in the data store
     */
    fun replaceResult(scenario: Scenario, newResult: IResultData) {
        if (!containsResult(scenario)) {
            error("Result for $scenario not in data store")
        }
        cachedResults[scenario] = newResult
    }

    /**
     * @return the result for [scenario]
     * @throws IllegalStateException is [scenario] doesn't exist in the data store
     */
    fun getResult(scenario: Scenario): IResultData =
        cachedResults[scenario] ?: error("No value for $scenario")

    /** @return if the store has results for [scenario] */
    fun containsFlickerServiceResult(scenario: Scenario): Boolean =
        cachedFlickerServiceAssertions.containsKey(scenario)

    fun addFlickerServiceAssertions(
        scenario: Scenario,
        groupedAssertions: Map<ScenarioInstance, Collection<ScenarioAssertion>>
    ) {
        if (containsFlickerServiceResult(scenario)) {
            error("Result for $scenario already in data store")
        }
        cachedFlickerServiceAssertions[scenario] = groupedAssertions
    }

    fun getFlickerServiceAssertions(
        scenario: Scenario
    ): Map<ScenarioInstance, Collection<ScenarioAssertion>> {
        return cachedFlickerServiceAssertions[scenario]
            ?: error("No flicker service results for $scenario")
    }
}
