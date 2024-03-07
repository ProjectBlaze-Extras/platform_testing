package android.tools.common.flicker.config.ime

import android.tools.common.flicker.assertors.ComponentTemplate
import android.tools.common.traces.component.ComponentNameMatcher

object Components {
    val IME = ComponentTemplate("IME") { ComponentNameMatcher.IME }
}
