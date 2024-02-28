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

package android.tools.common.flicker.config

import android.tools.common.flicker.AssertionInvocationGroup
import android.tools.common.flicker.assertors.assertions.AppLayerBecomesInvisible
import android.tools.common.flicker.assertors.assertions.AppLayerBecomesVisible
import android.tools.common.flicker.assertors.assertions.AppLayerCoversFullScreenAtEnd
import android.tools.common.flicker.assertors.assertions.AppLayerCoversFullScreenAtStart
import android.tools.common.flicker.assertors.assertions.AppLayerIsInvisibleAtEnd
import android.tools.common.flicker.assertors.assertions.AppLayerIsInvisibleAtStart
import android.tools.common.flicker.assertors.assertions.AppLayerIsVisibleAlways
import android.tools.common.flicker.assertors.assertions.AppLayerIsVisibleAtEnd
import android.tools.common.flicker.assertors.assertions.AppLayerIsVisibleAtStart
import android.tools.common.flicker.assertors.assertions.AppWindowBecomesInvisible
import android.tools.common.flicker.assertors.assertions.AppWindowBecomesTopWindow
import android.tools.common.flicker.assertors.assertions.AppWindowBecomesVisible
import android.tools.common.flicker.assertors.assertions.AppWindowIsInvisibleAtEnd
import android.tools.common.flicker.assertors.assertions.AppWindowIsInvisibleAtStart
import android.tools.common.flicker.assertors.assertions.AppWindowIsTopWindowAtStart
import android.tools.common.flicker.assertors.assertions.AppWindowIsVisibleAlways
import android.tools.common.flicker.assertors.assertions.AppWindowIsVisibleAtEnd
import android.tools.common.flicker.assertors.assertions.AppWindowIsVisibleAtStart
import android.tools.common.flicker.assertors.assertions.AppWindowOnTopAtEnd
import android.tools.common.flicker.assertors.assertions.AppWindowOnTopAtStart
import android.tools.common.flicker.assertors.assertions.EntireScreenCoveredAlways
import android.tools.common.flicker.assertors.assertions.FocusChanges
import android.tools.common.flicker.assertors.assertions.HasAtMostOneWindowMatching
import android.tools.common.flicker.assertors.assertions.LayerBecomesInvisible
import android.tools.common.flicker.assertors.assertions.LayerBecomesVisible
import android.tools.common.flicker.assertors.assertions.LayerReduces
import android.tools.common.flicker.assertors.assertions.ScreenLockedAtStart
import android.tools.common.flicker.assertors.assertions.SplitAppLayerBoundsBecomesVisible
import android.tools.common.flicker.assertors.assertions.VisibleLayersShownMoreThanOneConsecutiveEntry
import android.tools.common.flicker.assertors.assertions.VisibleWindowsShownMoreThanOneConsecutiveEntry
import android.tools.common.flicker.assertors.assertions.WindowBecomesPinned
import android.tools.common.flicker.assertors.assertions.WindowRemainInsideVisibleBounds
import android.tools.common.flicker.config.appclose.Components.CLOSING_APP
import android.tools.common.flicker.config.applaunch.Components.OPENING_APP
import android.tools.common.flicker.config.common.Components.LAUNCHER
import android.tools.common.flicker.config.pip.Components.PIP_APP
import android.tools.common.flicker.config.pip.Components.PIP_DISMISS_OVERLAY
import android.tools.common.flicker.config.splitscreen.Components.SPLIT_SCREEN_DIVIDER
import android.tools.common.flicker.config.splitscreen.Components.SPLIT_SCREEN_PRIMARY_APP
import android.tools.common.flicker.config.splitscreen.Components.SPLIT_SCREEN_SECONDARY_APP
import android.tools.common.traces.component.ComponentNameMatcher

object AssertionTemplates {
    val ENTIRE_TRACE_ASSERTIONS =
        listOf(
                EntireScreenCoveredAlways(),
                VisibleWindowsShownMoreThanOneConsecutiveEntry(),
                // Temporarily ignore these layers which might be visible for a single entry
                // and contain only view level changes during that entry (b/286054008)
                VisibleLayersShownMoreThanOneConsecutiveEntry(
                    ignore =
                        listOf(
                            ComponentNameMatcher.NOTIFICATION_SHADE,
                            ComponentNameMatcher.VOLUME_DIALOG,
                        )
                ),
            )
            .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val COMMON_ASSERTIONS =
        listOf(
                EntireScreenCoveredAlways(),
                VisibleWindowsShownMoreThanOneConsecutiveEntry(),
                VisibleLayersShownMoreThanOneConsecutiveEntry(),
            )
            .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val APP_LAUNCH_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                    AppLayerIsInvisibleAtStart(OPENING_APP),
                    AppLayerIsVisibleAtEnd(OPENING_APP),
                    AppLayerBecomesVisible(OPENING_APP),
                    AppWindowBecomesVisible(OPENING_APP),
                    AppWindowBecomesTopWindow(OPENING_APP),
                )
                .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val APP_CLOSE_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                    AppLayerIsVisibleAtStart(CLOSING_APP),
                    AppLayerIsInvisibleAtEnd(CLOSING_APP),
                    AppWindowIsVisibleAtStart(CLOSING_APP),
                    AppWindowIsInvisibleAtEnd(CLOSING_APP),
                    AppLayerBecomesInvisible(CLOSING_APP),
                    AppWindowBecomesInvisible(CLOSING_APP),
                    AppWindowIsTopWindowAtStart(CLOSING_APP),
                )
                .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val APP_LAUNCH_FROM_HOME_ASSERTIONS =
        APP_LAUNCH_ASSERTIONS +
            listOf(
                    AppLayerIsVisibleAtStart(LAUNCHER),
                    AppLayerIsInvisibleAtEnd(LAUNCHER),
                )
                .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val APP_LAUNCH_FROM_LOCK_ASSERTIONS =
        APP_LAUNCH_ASSERTIONS +
            listOf(FocusChanges(toComponent = OPENING_APP), ScreenLockedAtStart())
                .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val APP_CLOSE_TO_HOME_ASSERTIONS =
        APP_CLOSE_ASSERTIONS +
            listOf(
                    AppLayerIsInvisibleAtStart(LAUNCHER),
                    AppLayerIsVisibleAtEnd(LAUNCHER),
                    AppWindowIsInvisibleAtStart(LAUNCHER),
                    AppWindowIsVisibleAtEnd(LAUNCHER),
                    AppWindowBecomesTopWindow(LAUNCHER),
                )
                .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val APP_LAUNCH_FROM_NOTIFICATION_ASSERTIONS = COMMON_ASSERTIONS + APP_LAUNCH_ASSERTIONS

    val LAUNCHER_QUICK_SWITCH_ASSERTIONS =
        COMMON_ASSERTIONS +
            APP_LAUNCH_ASSERTIONS +
            APP_CLOSE_ASSERTIONS +
            listOf(
                    AppLayerCoversFullScreenAtStart(CLOSING_APP),
                    AppLayerCoversFullScreenAtEnd(OPENING_APP),
                    AppWindowOnTopAtStart(CLOSING_APP),
                    AppWindowOnTopAtEnd(OPENING_APP),
                    AppWindowBecomesInvisible(CLOSING_APP),
                    AppLayerBecomesInvisible(CLOSING_APP),
                    AppWindowBecomesVisible(OPENING_APP),
                    AppLayerBecomesVisible(OPENING_APP),
                )
                .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val APP_CLOSE_TO_PIP_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                    LayerReduces(PIP_APP),
                    FocusChanges(),
                    AppWindowIsVisibleAlways(PIP_APP),
                    WindowRemainInsideVisibleBounds(PIP_APP),
                    WindowBecomesPinned(PIP_APP),
                    LayerBecomesVisible(LAUNCHER),
                    HasAtMostOneWindowMatching(PIP_DISMISS_OVERLAY)
                )
                .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val ENTER_SPLITSCREEN_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                    LayerBecomesVisible(SPLIT_SCREEN_DIVIDER),
                    AppLayerIsVisibleAtEnd(SPLIT_SCREEN_PRIMARY_APP),
                    AppLayerBecomesVisible(SPLIT_SCREEN_SECONDARY_APP),
                    SplitAppLayerBoundsBecomesVisible(
                        SPLIT_SCREEN_PRIMARY_APP,
                        isPrimaryApp = true
                    ),
                    SplitAppLayerBoundsBecomesVisible(
                        SPLIT_SCREEN_SECONDARY_APP,
                        isPrimaryApp = false
                    ),
                    AppWindowBecomesVisible(SPLIT_SCREEN_PRIMARY_APP),
                    AppWindowBecomesVisible(SPLIT_SCREEN_SECONDARY_APP),
                )
                .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val EXIT_SPLITSCREEN_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                    LayerBecomesInvisible(SPLIT_SCREEN_DIVIDER),
                    AppLayerBecomesInvisible(SPLIT_SCREEN_PRIMARY_APP),
                    AppLayerIsVisibleAlways(SPLIT_SCREEN_SECONDARY_APP),
                    AppWindowBecomesInvisible(SPLIT_SCREEN_PRIMARY_APP),
                    AppWindowIsVisibleAlways(SPLIT_SCREEN_SECONDARY_APP),
                )
                .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })

    val RESIZE_SPLITSCREEN_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                    AppLayerIsVisibleAlways(SPLIT_SCREEN_PRIMARY_APP),
                    AppLayerIsVisibleAlways(SPLIT_SCREEN_SECONDARY_APP),
                    AppWindowIsVisibleAlways(SPLIT_SCREEN_PRIMARY_APP),
                    AppWindowIsVisibleAlways(SPLIT_SCREEN_SECONDARY_APP),
                )
                .associateBy({ it }, { AssertionInvocationGroup.BLOCKING })
}
