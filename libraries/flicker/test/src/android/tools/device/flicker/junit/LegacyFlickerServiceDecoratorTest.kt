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

import android.annotation.SuppressLint
import android.tools.TEST_SCENARIO
import android.tools.device.flicker.datastore.DataStore
import android.tools.device.flicker.isShellTransitionsEnabled
import android.tools.device.flicker.legacy.LegacyFlickerTest
import android.tools.rules.CleanFlickerEnvironmentRule
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runners.model.TestClass
import org.junit.runners.parameterized.TestWithParameters
import org.mockito.Mockito

/** Tests for [LegacyFlickerServiceDecorator] */
@SuppressLint("VisibleForTests")
class LegacyFlickerServiceDecoratorTest {
    @Before
    fun setup() {
        DataStore.clear()
    }

    @Test
    fun passValidClass() {
        val test =
            TestWithParameters(
                "test",
                TestClass(TestUtils.DummyTestClassValid::class.java),
                listOf(TestUtils.VALID_ARGS_EMPTY)
            )
        val mockTransitionRunner = Mockito.mock(ITransitionRunner::class.java)
        val decorator =
            LegacyFlickerServiceDecorator(
                test.testClass,
                scenario = null,
                mockTransitionRunner,
                inner = null
            )
        var failures = decorator.doValidateConstructor()
        Truth.assertWithMessage("Failure count").that(failures).isEmpty()

        failures = decorator.doValidateInstanceMethods()
        Truth.assertWithMessage("Failure count").that(failures).isEmpty()
    }

    @Test
    fun hasUniqueMethodNames() {
        val test =
            TestWithParameters(
                "test",
                TestClass(LegacyFlickerJUnit4ClassRunnerTest.SimpleFaasTest::class.java),
                listOf(TestUtils.VALID_ARGS_EMPTY)
            )
        val transitionRunner = LegacyFlickerJUnit4ClassRunner(test, TEST_SCENARIO).transitionRunner
        val decorator =
            LegacyFlickerServiceDecorator(
                test.testClass,
                TEST_SCENARIO,
                transitionRunner,
                inner = null
            )
        val methods =
            decorator.getTestMethods(
                LegacyFlickerJUnit4ClassRunnerTest.SimpleFaasTest(LegacyFlickerTest())
            )
        val duplicatedMethods = methods.groupBy { it.name }.filter { it.value.size > 1 }

        if (isShellTransitionsEnabled) {
            Truth.assertWithMessage("Methods").that(methods).isNotEmpty()
        }
        Truth.assertWithMessage("Unique methods").that(duplicatedMethods).isEmpty()
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
