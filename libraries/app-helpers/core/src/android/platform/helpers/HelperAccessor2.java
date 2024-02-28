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
package android.platform.helpers;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * @param <T> the helper interface under test.
 *     <p>A {@code HelperAccessor} can be included in any test to access an App Helper
 *     implementation.
 *     <p>For example: <code>
 *     {@code HelperAccessor<IXHelper> accessor = new HelperAccessor(IXHelper.class);}
 *     accessor.get().performSomeAction();
 * </code> To target a specific helper implementation by prefix, build this object and call, <code>
 * withPrefix</code> on it.
 */
public class HelperAccessor2<T extends IAppHelper2> {
    private final Class<T> mInterfaceClass;

    private T mHelper;
    private String mPrefix;

    public HelperAccessor2(Class<T> klass) {
        mInterfaceClass = klass;
    }

    /** Selects only helpers that begin with the prefix, {@code prefix}. */
    public HelperAccessor2<T> withPrefix(String prefix) {
        mPrefix = prefix;
        // Unset the helper, in case this was changed after first use.
        mHelper = null;
        // Return self to follow a pseudo-builder initialization pattern.
        return this;
    }

    /** accessor.get().performSomeAction() {@code}. */
    public T get() {
        if (mHelper == null) {
            if (mPrefix == null || mPrefix.isEmpty()) {
                mHelper =
                        HelperManager.getInstance(
                                        InstrumentationRegistry.getInstrumentation().getContext(),
                                        InstrumentationRegistry.getInstrumentation())
                                .get(mInterfaceClass);
            } else {
                mHelper =
                        HelperManager.getInstance(
                                        InstrumentationRegistry.getInstrumentation().getContext(),
                                        InstrumentationRegistry.getInstrumentation())
                                .get(mInterfaceClass, mPrefix);
            }
        }
        return mHelper;
    }
}
