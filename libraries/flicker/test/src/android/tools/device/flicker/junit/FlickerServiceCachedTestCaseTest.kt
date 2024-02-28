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
import android.device.collectors.util.SendToInstrumentation
import android.tools.assertThrows
import android.tools.common.flicker.AssertionInvocationGroup
import android.tools.common.flicker.FlickerService
import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.assertions.AssertionData
import android.tools.common.flicker.assertions.AssertionResult
import android.tools.common.flicker.assertions.ScenarioAssertion
import android.tools.common.flicker.assertions.SubjectsParser
import android.tools.common.io.Reader
import android.tools.device.flicker.FlickerServiceResultsCollector
import android.tools.utils.KotlinMockito
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.Description
import org.mockito.Mockito

class FlickerServiceCachedTestCaseTest {
    @Test
    fun reportsPassingResultMetric() {
        val decorator = Mockito.mock(IFlickerJUnitDecorator::class.java)
        val mockInstrumentation = Mockito.mock(Instrumentation::class.java)
        val mockFlickerService = Mockito.mock(FlickerService::class.java)
        val mockScenarioInstance = Mockito.mock(ScenarioInstance::class.java)
        val mockScenarioAssertion = Mockito.mock(ScenarioAssertion::class.java)
        val assertionResult =
            object : AssertionResult {
                override val name: String = "MOCK_SCENARIO#mockAssertion"
                override val assertionData =
                    arrayOf<AssertionData>(
                        object : AssertionData {
                            override fun checkAssertion(run: SubjectsParser) {
                                error("Unimplemented - shouldn't be called")
                            }
                        }
                    )
                override val assertionErrors = emptyArray<Throwable>()
                override val stabilityGroup = AssertionInvocationGroup.BLOCKING
                override val passed = true
            }
        Mockito.`when`(mockScenarioAssertion.execute()).thenReturn(assertionResult)
        Mockito.`when`(mockScenarioInstance.generateAssertions())
            .thenReturn(listOf(mockScenarioAssertion))
        Mockito.`when`(mockFlickerService.detectScenarios(KotlinMockito.any(Reader::class.java)))
            .thenReturn(listOf(mockScenarioInstance))

        val mockDescription = Mockito.mock(Description::class.java)
        val test =
            FlickerServiceCachedTestCase(
                assertion = mockScenarioAssertion,
                method = InjectedTestCase::class.java.getMethod("execute", Description::class.java),
                onlyBlocking = false,
                isLast = false,
                injectedBy = decorator,
                instrumentation = mockInstrumentation,
                paramString = "",
            )
        test.execute(mockDescription)

        Mockito.verify(mockInstrumentation)
            .sendStatus(
                Mockito.eq(SendToInstrumentation.INST_STATUS_IN_PROGRESS),
                KotlinMockito.argThat {
                    this.getString(
                        "${FlickerServiceResultsCollector.FAAS_METRICS_PREFIX}::" +
                            assertionResult.name
                    ) == "0"
                }
            )
    }

    @Test
    fun reportsFailingResultMetric() {
        val decorator = Mockito.mock(IFlickerJUnitDecorator::class.java)
        val mockInstrumentation = Mockito.mock(Instrumentation::class.java)
        val mockFlickerService = Mockito.mock(FlickerService::class.java)
        val mockScenarioInstance = Mockito.mock(ScenarioInstance::class.java)
        val mockScenarioAssertion = Mockito.mock(ScenarioAssertion::class.java)
        val assertionResult =
            object : AssertionResult {
                override val name: String = "MY_CUSTOM_SCENARIO#myAssertion"
                override val assertionData =
                    arrayOf<AssertionData>(
                        object : AssertionData {
                            override fun checkAssertion(run: SubjectsParser) {
                                error("Unimplemented - shouldn't be called")
                            }
                        }
                    )
                override val assertionErrors = arrayOf<Throwable>(Exception("EXPECTED"))
                override val stabilityGroup = AssertionInvocationGroup.BLOCKING
                override val passed = false
            }
        Mockito.`when`(mockScenarioAssertion.execute()).thenReturn(assertionResult)
        Mockito.`when`(mockScenarioInstance.generateAssertions())
            .thenReturn(listOf(mockScenarioAssertion))
        Mockito.`when`(mockFlickerService.detectScenarios(KotlinMockito.any(Reader::class.java)))
            .thenReturn(listOf(mockScenarioInstance))

        val mockDescription = Mockito.mock(Description::class.java)
        val test =
            FlickerServiceCachedTestCase(
                assertion = mockScenarioAssertion,
                method = InjectedTestCase::class.java.getMethod("execute", Description::class.java),
                onlyBlocking = false,
                isLast = false,
                injectedBy = decorator,
                instrumentation = mockInstrumentation,
                paramString = "",
            )

        val failure = assertThrows<Throwable> { test.execute(mockDescription) }
        Truth.assertThat(failure).hasMessageThat().isEqualTo("EXPECTED")

        Mockito.verify(mockInstrumentation)
            .sendStatus(
                Mockito.eq(SendToInstrumentation.INST_STATUS_IN_PROGRESS),
                KotlinMockito.argThat {
                    this.getString(
                        "${FlickerServiceResultsCollector.FAAS_METRICS_PREFIX}::" +
                            assertionResult.name
                    ) == "1"
                }
            )
    }
}
