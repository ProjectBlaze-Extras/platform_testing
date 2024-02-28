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

package android.tools.common.flicker.assertions

import android.tools.common.FLICKER_TAG
import android.tools.common.Logger
import android.tools.common.flicker.AssertionInvocationGroup
import android.tools.common.io.Reader

internal data class ScenarioAssertionImpl(
    private val name: String,
    private val reader: Reader,
    private val assertionData: Collection<AssertionData>,
    override val stabilityGroup: AssertionInvocationGroup,
    private val assertionRunner: AssertionRunner = ReaderAssertionRunner(reader)
) : ScenarioAssertion {
    init {
        require(assertionData.isNotEmpty()) { "Expected at least one assertion data object." }
    }

    override fun execute() =
        Logger.withTracing("executeAssertion") {
            AssertionResultImpl(
                    name,
                    assertionData.toTypedArray(),
                    assertionData.mapNotNull { assertionRunner.runAssertion(it) }.toTypedArray(),
                    stabilityGroup
                )
                .also { log(it) }
        }

    override fun toString() = name

    private fun log(result: AssertionResult) {
        if (result.failed) {
            Logger.w(
                "$FLICKER_TAG-SERVICE",
                "${result.name} FAILED :: " + result.assertionErrors.map { it.message }
            )
        } else {
            Logger.w("$FLICKER_TAG-SERVICE", "${result.name} PASSED")
        }
    }
}
