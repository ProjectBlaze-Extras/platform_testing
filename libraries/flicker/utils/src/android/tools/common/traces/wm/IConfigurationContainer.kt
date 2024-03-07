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

package android.tools.common.traces.wm

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Represents the configuration of an element in the window manager hierarchy
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot access internal
 * Java/Android functionality
 */
@JsExport
interface IConfigurationContainer {
    @JsName("overrideConfiguration") val overrideConfiguration: Configuration?

    @JsName("fullConfiguration") val fullConfiguration: Configuration?

    @JsName("mergedOverrideConfiguration") val mergedOverrideConfiguration: Configuration?

    @JsName("windowingMode") val windowingMode: Int

    @JsName("activityType") val activityType: Int

    @JsName("isEmpty") val isEmpty: Boolean
}
