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

package android.tools.common

import org.junit.Assert
import org.junit.Test

class LoggerBuilderTest {

    @Test
    fun testOnV() {
        var actualTag = ""
        var actualMsg = ""
        val logger =
            LoggerBuilder()
                .setV { tag, msg ->
                    actualTag = tag
                    actualMsg = msg
                }
                .build()
        logger.v(EXPECTED_TAG, EXPECTED_MSG)
        logger.d(ERROR, ERROR)
        logger.i(ERROR, ERROR)
        logger.w(ERROR, ERROR)
        logger.e(ERROR, ERROR)
        Assert.assertEquals(actualTag, EXPECTED_TAG)
        Assert.assertEquals(actualMsg, EXPECTED_MSG)
    }

    @Test
    fun testOnD() {
        var actualTag = ""
        var actualMsg = ""
        val logger =
            LoggerBuilder()
                .setD { tag, msg ->
                    actualTag = tag
                    actualMsg = msg
                }
                .build()
        logger.d(EXPECTED_TAG, EXPECTED_MSG)
        logger.v(ERROR, ERROR)
        logger.i(ERROR, ERROR)
        logger.w(ERROR, ERROR)
        logger.e(ERROR, ERROR)
        Assert.assertEquals(actualTag, EXPECTED_TAG)
        Assert.assertEquals(actualMsg, EXPECTED_MSG)
    }

    @Test
    fun testOnI() {
        var actualTag = ""
        var actualMsg = ""
        val logger =
            LoggerBuilder()
                .setI { tag, msg ->
                    actualTag = tag
                    actualMsg = msg
                }
                .build()
        logger.i(EXPECTED_TAG, EXPECTED_MSG)
        logger.d(ERROR, ERROR)
        logger.v(ERROR, ERROR)
        logger.w(ERROR, ERROR)
        logger.e(ERROR, ERROR)
        Assert.assertEquals(actualTag, EXPECTED_TAG)
        Assert.assertEquals(actualMsg, EXPECTED_MSG)
    }

    @Test
    fun testOnW() {
        var actualTag = ""
        var actualMsg = ""
        val logger =
            LoggerBuilder()
                .setW { tag, msg ->
                    actualTag = tag
                    actualMsg = msg
                }
                .build()
        logger.w(EXPECTED_TAG, EXPECTED_MSG)
        logger.v(ERROR, ERROR)
        logger.d(ERROR, ERROR)
        logger.i(ERROR, ERROR)
        logger.e(ERROR, ERROR)
        Assert.assertEquals(actualTag, EXPECTED_TAG)
        Assert.assertEquals(actualMsg, EXPECTED_MSG)
    }

    @Test
    fun testOnEWithoutException() {
        var actualTag = ""
        var actualMsg = ""
        var actualError: Throwable? = Exception(ERROR)
        val logger =
            LoggerBuilder()
                .setE { tag, msg, error ->
                    actualTag = tag
                    actualMsg = msg
                    actualError = error
                }
                .build()
        logger.e(EXPECTED_TAG, EXPECTED_MSG)
        logger.v(ERROR, ERROR)
        logger.d(ERROR, ERROR)
        logger.i(ERROR, ERROR)
        logger.w(ERROR, ERROR)
        Assert.assertEquals(actualTag, EXPECTED_TAG)
        Assert.assertEquals(actualMsg, EXPECTED_MSG)
        Assert.assertNull(actualError)
    }

    @Test
    fun testOnEWithException() {
        var actualTag = ""
        var actualMsg = ""
        var actualError: Throwable? = Exception(ERROR)
        val logger =
            LoggerBuilder()
                .setE { tag, msg, error ->
                    actualTag = tag
                    actualMsg = msg
                    actualError = error
                }
                .build()
        logger.e(EXPECTED_TAG, EXPECTED_MSG, EXPECTED_ERROR)
        logger.v(ERROR, ERROR)
        logger.d(ERROR, ERROR)
        logger.i(ERROR, ERROR)
        logger.w(ERROR, ERROR)
        Assert.assertEquals(actualTag, EXPECTED_TAG)
        Assert.assertEquals(actualMsg, EXPECTED_MSG)
        Assert.assertSame(actualError, EXPECTED_ERROR)
    }

    companion object {
        const val EXPECTED_TAG = "TAG1"
        const val EXPECTED_MSG = "MSG1"
        const val ERROR = "ERROR"
        val EXPECTED_ERROR = Exception("EXPECTED")
    }
}
