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

import android.tools.common.flicker.AssertionInvocationGroup

/** Base class for a FaaS assertion */
internal data class AssertionResultImpl(
    override val name: String,
    override val assertionData: Array<AssertionData>,
    override val assertionErrors: Array<Throwable>,
    override val stabilityGroup: AssertionInvocationGroup
) : AssertionResult {
    // Overriding equals because of use of Array
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssertionResultImpl) return false

        if (name != other.name) return false
        if (!assertionData.contentEquals(other.assertionData)) return false
        if (!assertionErrors.contentEquals(other.assertionErrors)) return false
        if (stabilityGroup != other.stabilityGroup) return false

        return true
    }

    // Overriding hashCode because of use of Array
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + assertionData.contentHashCode()
        result = 31 * result + assertionErrors.contentHashCode()
        result = 31 * result + stabilityGroup.hashCode()
        return result
    }
}
