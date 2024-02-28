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

package android.tools.device.flicker.junit

import android.app.Instrumentation
import android.tools.common.Timestamps
import android.tools.common.flicker.AssertionInvocationGroup
import android.tools.common.flicker.FlickerConfig
import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.annotation.ExpectedScenarios
import android.tools.common.flicker.annotation.FlickerConfigProvider
import android.tools.common.flicker.assertions.FlickerTest
import android.tools.common.flicker.assertors.AssertionTemplate
import android.tools.common.flicker.config.FlickerConfig
import android.tools.common.flicker.config.FlickerConfigEntry
import android.tools.common.flicker.config.ScenarioId
import android.tools.common.flicker.extractors.ScenarioExtractor
import android.tools.common.flicker.extractors.TraceSlice
import android.tools.common.io.Reader
import android.tools.device.apphelpers.BrowserAppHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runner.notification.RunNotifier

class FlickerServiceJUnit4ClassRunnerTest {
    @Test
    fun runsTheTestRules() {
        testRuleRunCount = 0
        val runner = FlickerServiceJUnit4ClassRunner(SimpleTest::class.java)
        runner.run(RunNotifier())

        Truth.assertThat(testRuleRunCount).isEqualTo(1)
    }

    /** Below are all the mock test classes uses for testing purposes */
    @RunWith(FlickerServiceJUnit4ClassRunner::class)
    open class SimpleTest {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        private val testApp: BrowserAppHelper = BrowserAppHelper(instrumentation)

        @get:Rule
        val myRule = TestRule { base, _ ->
            testRuleRunCount++
            base
        }

        @Test
        @ExpectedScenarios(["MY_CUSTOM_SCENARIO"])
        fun test() {
            testApp.open()
        }

        companion object {
            @FlickerConfigProvider
            @JvmStatic
            fun flickerConfigProvider(): FlickerConfig {
                return FlickerConfig()
                    .use(
                        FlickerConfigEntry(
                            scenarioId = ScenarioId("MY_CUSTOM_SCENARIO"),
                            extractor =
                                object : ScenarioExtractor {
                                    override fun extract(reader: Reader): List<TraceSlice> {
                                        return listOf(
                                            TraceSlice(Timestamps.min(), Timestamps.max())
                                        )
                                    }
                                },
                            assertions =
                                mapOf(
                                    object : AssertionTemplate("myAssertion") {
                                        override fun doEvaluate(
                                            scenarioInstance: ScenarioInstance,
                                            flicker: FlickerTest
                                        ) {
                                            flicker.assertWm {
                                                // Random test
                                                visibleWindowsShownMoreThanOneConsecutiveEntry()
                                            }
                                        }
                                    } to AssertionInvocationGroup.BLOCKING
                                ),
                            enabled = true
                        )
                    )
            }
        }
    }

    companion object {
        var testRuleRunCount = 0
    }
}
