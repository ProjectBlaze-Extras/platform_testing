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

package android.tools.common.traces

import android.tools.assertThrows
import android.tools.common.ITrace
import android.tools.common.ITraceEntry
import android.tools.common.Timestamp
import android.tools.common.Timestamps
import com.google.common.truth.Truth
import org.junit.Test

/** To run this test: `atest FlickerLibTest:ITraceTest` */
class ITraceTest {
    @Test
    fun getEntryExactlyAtTest() {
        val entry1 = SimpleTraceEntry(Timestamps.from(1, 1, 1))
        val entry2 = SimpleTraceEntry(Timestamps.from(5, 5, 5))
        val entry3 = SimpleTraceEntry(Timestamps.from(25, 25, 25))
        val trace = SimpleTrace(arrayOf(entry1, entry2, entry3))

        Truth.assertThat(trace.getEntryExactlyAt(Timestamps.from(1, 1, 1))).isEqualTo(entry1)
        Truth.assertThat(trace.getEntryExactlyAt(Timestamps.from(5, 5, 5))).isEqualTo(entry2)
        Truth.assertThat(trace.getEntryExactlyAt(Timestamps.from(25, 25, 25))).isEqualTo(entry3)

        Truth.assertThat(
                assertThrows<Throwable> { trace.getEntryExactlyAt(Timestamps.from(6, 6, 6)) }
            )
            .hasMessageThat()
            .contains("does not exist")
    }

    @Test
    fun getEntryAtTest() {
        val entry1 = SimpleTraceEntry(Timestamps.from(2, 2, 2))
        val entry2 = SimpleTraceEntry(Timestamps.from(5, 5, 5))
        val entry3 = SimpleTraceEntry(Timestamps.from(25, 25, 25))
        val trace = SimpleTrace(arrayOf(entry1, entry2, entry3))

        Truth.assertThat(trace.getEntryAt(Timestamps.from(2, 2, 2))).isEqualTo(entry1)
        Truth.assertThat(trace.getEntryAt(Timestamps.from(5, 5, 5))).isEqualTo(entry2)
        Truth.assertThat(trace.getEntryAt(Timestamps.from(25, 25, 25))).isEqualTo(entry3)

        Truth.assertThat(trace.getEntryAt(Timestamps.from(4, 4, 4))).isEqualTo(entry1)
        Truth.assertThat(trace.getEntryAt(Timestamps.from(6, 6, 6))).isEqualTo(entry2)
        Truth.assertThat(trace.getEntryAt(Timestamps.from(100, 100, 100))).isEqualTo(entry3)

        Truth.assertThat(assertThrows<Throwable> { trace.getEntryAt(Timestamps.from(1, 1, 1)) })
            .hasMessageThat()
            .contains("No entry at or before timestamp")
    }

    class SimpleTraceEntry(override val timestamp: Timestamp) : ITraceEntry

    class SimpleTrace(override val entries: Array<ITraceEntry>) : ITrace<ITraceEntry> {
        override fun slice(
            startTimestamp: Timestamp,
            endTimestamp: Timestamp
        ): ITrace<ITraceEntry> {
            error("Not yet implemented")
        }
    }
}
