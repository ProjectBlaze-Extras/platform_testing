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

import android.tools.common.datatypes.Rect
import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
interface IWindowContainer : IConfigurationContainer {
    @JsName("title") val title: String

    @JsName("token") val token: String

    @JsName("orientation") val orientation: Int

    @JsName("layerId") val layerId: Int

    @JsName("children") val children: Array<IWindowContainer>

    @JsName("computedZ") val computedZ: Int

    @JsName("isVisible") val isVisible: Boolean

    @JsName("name") val name: String

    @JsName("stableId") val stableId: String
    val isFullscreen: Boolean

    @JsName("bounds") val bounds: Rect

    @JsName("parent") var parent: IWindowContainer?
}
