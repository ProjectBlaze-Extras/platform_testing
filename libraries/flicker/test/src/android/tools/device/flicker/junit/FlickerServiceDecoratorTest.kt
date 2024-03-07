package android.tools.device.flicker.junit

import android.app.Instrumentation
import android.os.Bundle
import android.tools.common.CrossPlatform
import android.tools.common.Rotation
import android.tools.common.flicker.AssertionInvocationGroup
import android.tools.common.flicker.FlickerConfig
import android.tools.common.flicker.FlickerService
import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.ScenarioInstanceImpl
import android.tools.common.flicker.annotation.ExpectedScenarios
import android.tools.common.flicker.assertions.FlickerTest
import android.tools.common.flicker.assertors.AssertionTemplate
import android.tools.common.flicker.config.FlickerConfigEntry
import android.tools.common.flicker.config.FlickerServiceConfig
import android.tools.common.flicker.config.ScenarioId
import android.tools.common.flicker.extractors.ScenarioExtractor
import android.tools.common.io.Reader
import android.tools.utils.KotlinMockito
import android.tools.utils.assertThrows
import android.util.Log
import com.google.common.truth.Truth
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass
import org.mockito.Mockito

class FlickerServiceDecoratorTest {
    @Test
    fun sendsInstrumentationUpdatesOWhenComputingTestMethods() {
        val instrumentation = Mockito.mock(Instrumentation::class.java)
        val testClass = Mockito.mock(TestClass::class.java)
        val innerDecorator = Mockito.mock(IFlickerJUnitDecorator::class.java)
        val method = Mockito.mock(FrameworkMethod::class.java)
        val flickerService = Mockito.mock(FlickerService::class.java)

        val flickerConfigProviderMethods = Mockito.mock(FrameworkMethod::class.java)

        Mockito.`when`(testClass.getAnnotatedMethods(ExpectedScenarios::class.java))
            .thenReturn(listOf(method))
        Mockito.`when`(
                testClass.getAnnotatedMethods(
                    android.tools.common.flicker.annotation.FlickerConfigProvider::class.java
                )
            )
            .thenReturn(listOf(flickerConfigProviderMethods))
        Mockito.`when`(flickerConfigProviderMethods.invokeExplosively(testClass))
            .thenReturn(FlickerConfig().use(FlickerServiceConfig.DEFAULT))
        Mockito.`when`(method.annotations).thenReturn(emptyArray())
        Mockito.`when`(innerDecorator.getTestMethods(KotlinMockito.any(Object::class.java)))
            .thenReturn(listOf(method))

        val test = Mockito.mock(Object::class.java)
        val decorator =
            FlickerServiceDecorator(
                testClass = testClass,
                paramString = null,
                skipNonBlocking = false,
                inner = innerDecorator,
                instrumentation = instrumentation,
                flickerService = flickerService
            )
        decorator.getTestMethods(test)

        Mockito.verify(instrumentation)
            .sendStatus(
                Mockito.anyInt(),
                KotlinMockito.argThat<Bundle> {
                    this.getString(Instrumentation.REPORT_KEY_STREAMRESULT)
                        ?.contains("Running setup")
                        ?: false
                }
            )
        Mockito.verify(instrumentation)
            .sendStatus(
                Mockito.anyInt(),
                KotlinMockito.argThat {
                    this.getString(Instrumentation.REPORT_KEY_STREAMRESULT)
                        ?.contains("Running transition")
                        ?: false
                }
            )
        Mockito.verify(instrumentation)
            .sendStatus(
                Mockito.anyInt(),
                KotlinMockito.argThat {
                    this.getString(Instrumentation.REPORT_KEY_STREAMRESULT)
                        ?.contains("Running teardown")
                        ?: false
                }
            )
    }

    @Test
    fun failsTestAndNotModuleOnFlickerServiceMethodComputeError() {
        val flickerMethodComputeError = Throwable("Flicker Method Compute Error")

        val instrumentation = Mockito.mock(Instrumentation::class.java)
        val testClass = Mockito.mock(TestClass::class.java)
        val innerDecorator = Mockito.mock(IFlickerJUnitDecorator::class.java)
        val method = Mockito.mock(FrameworkMethod::class.java)
        val flickerService = Mockito.mock(FlickerService::class.java)
        val mockDescription = Mockito.mock(Description::class.java)

        val flickerConfigProviderMethods = Mockito.mock(FrameworkMethod::class.java)

        Mockito.`when`(testClass.getAnnotatedMethods(ExpectedScenarios::class.java))
            .thenReturn(listOf(method))
        Mockito.`when`(
                testClass.getAnnotatedMethods(
                    android.tools.common.flicker.annotation.FlickerConfigProvider::class.java
                )
            )
            .thenReturn(listOf(flickerConfigProviderMethods))
        Mockito.`when`(flickerConfigProviderMethods.invokeExplosively(testClass))
            .thenReturn(FlickerConfig().use(FlickerServiceConfig.DEFAULT))
        Mockito.`when`(method.annotations).thenReturn(emptyArray())
        Mockito.`when`(innerDecorator.getTestMethods(KotlinMockito.any(Object::class.java)))
            .thenReturn(listOf(method))
        Mockito.`when`(
                innerDecorator.getChildDescription(KotlinMockito.any(FrameworkMethod::class.java))
            )
            .thenReturn(mockDescription)

        Mockito.`when`(flickerService.detectScenarios(KotlinMockito.any(Reader::class.java))).then {
            throw flickerMethodComputeError
        }

        val test = Mockito.mock(Object::class.java)
        val decorator =
            FlickerServiceDecorator(
                testClass = testClass,
                paramString = null,
                skipNonBlocking = false,
                inner = innerDecorator,
                instrumentation = instrumentation,
                flickerService = flickerService
            )

        // This should not throw
        decorator.getTestMethods(test)

        val methodInvoker = decorator.getMethodInvoker(method, test)
        val exception = assertThrows<Throwable> { methodInvoker.evaluate() }
        Truth.assertThat(exception.stackTraceToString()).contains(flickerMethodComputeError.message)
    }

    // TODO(b/303426072): fix flaky test
    @Test
    @Ignore
    fun handleDuplicateFlickerMethods() {
        val instrumentation = Mockito.mock(Instrumentation::class.java)
        val testClass = Mockito.mock(TestClass::class.java)
        val innerDecorator = Mockito.mock(IFlickerJUnitDecorator::class.java)
        val method = Mockito.mock(FrameworkMethod::class.java)
        val flickerService = Mockito.mock(FlickerService::class.java)
        val mockDescription = Mockito.mock(Description::class.java)
        val flickerConfigProviderMethods = Mockito.mock(FrameworkMethod::class.java)

        Mockito.`when`(testClass.getAnnotatedMethods(ExpectedScenarios::class.java))
            .thenReturn(listOf(method))
        Mockito.`when`(
                testClass.getAnnotatedMethods(
                    android.tools.common.flicker.annotation.FlickerConfigProvider::class.java
                )
            )
            .thenReturn(listOf(flickerConfigProviderMethods))
        Mockito.`when`(flickerConfigProviderMethods.invokeExplosively(testClass))
            .thenReturn(FlickerConfig().use(FlickerServiceConfig.DEFAULT))
        Mockito.`when`(method.annotations).thenReturn(emptyArray())
        Mockito.`when`(innerDecorator.getTestMethods(KotlinMockito.any(Object::class.java)))
            .thenReturn(listOf(method))
        Mockito.`when`(
                innerDecorator.getChildDescription(KotlinMockito.any(FrameworkMethod::class.java))
            )
            .thenReturn(mockDescription)

        val mockConfig =
            FlickerConfigEntry(
                scenarioId = ScenarioId("MY_MOCK_SCENARIO"),
                extractor = Mockito.mock(ScenarioExtractor::class.java),
                assertions =
                    mapOf(
                        object : AssertionTemplate("myMockAssertion") {
                            override fun doEvaluate(
                                scenarioInstance: ScenarioInstance,
                                flicker: FlickerTest
                            ) {
                                flicker.assertLayers {
                                    // Does nothing
                                }
                            }
                        } to AssertionInvocationGroup.BLOCKING
                    ),
                enabled = true
            )

        val mockScenarioInstance1 =
            ScenarioInstanceImpl(
                config = mockConfig,
                startRotation = Rotation.ROTATION_0,
                endRotation = Rotation.ROTATION_0,
                startTimestamp = CrossPlatform.timestamp.from(10),
                endTimestamp = CrossPlatform.timestamp.from(20),
                reader = Mockito.mock(Reader::class.java)
            )

        val mockScenarioInstance2 =
            ScenarioInstanceImpl(
                config = mockConfig,
                startRotation = Rotation.ROTATION_0,
                endRotation = Rotation.ROTATION_0,
                startTimestamp = CrossPlatform.timestamp.from(10),
                endTimestamp = CrossPlatform.timestamp.from(20),
                reader = Mockito.mock(Reader::class.java)
            )

        Mockito.`when`(flickerService.detectScenarios(KotlinMockito.any(Reader::class.java)))
            .thenReturn(listOf(mockScenarioInstance1, mockScenarioInstance2))

        val test = Mockito.mock(Object::class.java)
        val decorator =
            FlickerServiceDecorator(
                testClass = testClass,
                paramString = null,
                skipNonBlocking = false,
                inner = innerDecorator,
                instrumentation = instrumentation,
                flickerService = flickerService
            )

        val methods = decorator.getTestMethods(test)

        Truth.assertThat(methods.size).isAtLeast(2)
        val flickerTestMethods = methods.filterIsInstance<FlickerServiceCachedTestCase>()
        Truth.assertThat(flickerTestMethods).hasSize(2)
        Log.d("PABLOG", "flickerTestMethods = [${flickerTestMethods.joinToString { it.name }}]")
        Truth.assertThat(flickerTestMethods.distinctBy { it.name }).hasSize(2)
    }
}
