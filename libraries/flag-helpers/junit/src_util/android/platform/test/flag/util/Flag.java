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

package android.platform.test.flag.util;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * Contains the information of a flag.
 *
 * <p>There are two types of flags:
 * <li>Legacy flags: the format is {namespace}/{flagName}. For these flags, packageName and
 *     flagsClassName will be null, and both fullFlagName and simpleFlagName will be the flagName.
 * <li>AConfig flags: the format is {packageName}.{simpleFlagName}. For these flags, namespace will
 *     be null, fullFlagName will be {packageName}.{simpleFlagName}, and flagsClassName will be
 *     {packageName}.Flags.
 */
@AutoValue
public abstract class Flag {
    public static final String NAMESPACE_FLAG_SEPARATOR = "/";

    /** The format of flag with namespace is {namespace}/{flagName}. */
    public static final String FLAG_WITH_NAMESPACE_FORMAT = "%s/%s";

    /** The format of aconfig full flag name is {packageName}.{simpleFlagName}. */
    public static final String ACONFIG_FULL_FLAG_FORMAT = "%s.%s";

    private static final String PACKAGE_NAME_SIMPLE_NAME_SEPARATOR = ".";
    private static final String FLAGS_CLASS_FORMAT = "%s.Flags";

    public static Flag createFlag(String flag) {
        String namespace = null;
        String fullFlagName = null;
        String packageName = null;
        String simpleFlagName = null;
        if (flag.contains(NAMESPACE_FLAG_SEPARATOR)) {
            String[] flagSplits = flag.split(NAMESPACE_FLAG_SEPARATOR, /* limit= */ 2);
            namespace = flagSplits[0];
            fullFlagName = flagSplits[1];
            simpleFlagName = fullFlagName;
        } else {
            fullFlagName = flag;
            if (!fullFlagName.contains(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Flag %s is invalid. The format should be {packageName}"
                                        + ".{simpleFlagName}",
                                flag));
            }
            int index = fullFlagName.lastIndexOf(PACKAGE_NAME_SIMPLE_NAME_SEPARATOR);
            packageName = fullFlagName.substring(0, index);
            simpleFlagName = fullFlagName.substring(index + 1);
        }

        return new AutoValue_Flag(namespace, fullFlagName, packageName, simpleFlagName);
    }

    @Nullable
    public abstract String namespace();

    public abstract String fullFlagName();

    @Nullable
    public abstract String packageName();

    public abstract String simpleFlagName();

    @Nullable
    public String flagsClassName() {
        return packageName() == null ? null : String.format(FLAGS_CLASS_FORMAT, packageName());
    }
}
