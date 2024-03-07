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

package android.platform.test.flag.junit;

import android.platform.test.flag.util.Flag;
import android.platform.test.flag.util.FlagReadException;
import android.platform.test.flag.util.FlagSetException;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Sets;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A {@link TestRule} that helps to set flag values in unit test. */
public final class SetFlagsRule implements TestRule {
    private static final String FAKE_FEATURE_FLAGS_IMPL_CLASS_NAME = "FakeFeatureFlagsImpl";
    private static final String FEATURE_FLAGS_CLASS_NAME = "FeatureFlags";
    private static final String FEATURE_FLAGS_FIELD_NAME = "FEATURE_FLAGS";
    private static final String FLAGS_CLASS_NAME = "Flags";
    private static final String SET_FLAG_METHOD_NAME = "setFlag";
    private static final String RESET_ALL_METHOD_NAME = "resetAll";

    // Store instances for entire life of a SetFlagsRule instance
    private final Map<Class<?>, Object> mFlagsClassToFakeFlagsImpl = new HashMap<>();
    private final Map<Class<?>, Object> mFlagsClassToRealFlagsImpl = new HashMap<>();

    // Store value for the scope of each test method
    private final Map<Class<?>, Map<Flag, Boolean>> mFlagsClassToFlagDefaultMap = new HashMap<>();

    // Any flags added to this list cannot be set imperatively (i.e. with enableFlags/disableFlags)
    private final Set<String> mLockedFlagNames = new HashSet<>();

    private boolean mIsInitWithDefault = false;
    private FlagsParameterization mFlagsParameterization;
    private boolean mIsRuleEvaluating = false;

    /**
     * Enable default value for flags
     *
     * <p>Once this method is called the flag value in the same Flag class will be set as the same
     * value of current release configuration.
     *
     * <p>This methods should be called before calling enable/disableFlags
     *
     * @deprecated Prefer {@link #SetFlagsRule(SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT)}
     */
    @Deprecated
    public void initAllFlagsToReleaseConfigDefault() {
        if (!mIsInitWithDefault) {
            mFlagsClassToRealFlagsImpl.clear();
            mFlagsClassToFakeFlagsImpl.clear();
        }
        mIsInitWithDefault = true;
    }

    public enum DefaultInitValueType {
        /**
         * Initialize flag value as null
         *
         * <p>Flag value need to be set before using
         */
        NULL_DEFAULT,

        /**
         * Initialize flag value with the default value from the device
         *
         * <p>If flag value is not overridden by adb, then the default value is from the release
         * configuration when the test is built.
         */
        DEVICE_DEFAULT,
    }

    public SetFlagsRule() {
        this(DefaultInitValueType.NULL_DEFAULT);
    }

    public SetFlagsRule(DefaultInitValueType defaultType) {
        this(defaultType, null);
    }

    public SetFlagsRule(
            DefaultInitValueType defaultType,
            @Nullable FlagsParameterization flagsParameterization) {
        switch (defaultType) {
            case DEVICE_DEFAULT:
                mIsInitWithDefault = true;
                break;
            default:
                break;
        }
        mFlagsParameterization = flagsParameterization;
        if (flagsParameterization != null) {
            mLockedFlagNames.addAll(flagsParameterization.mOverrides.keySet());
        }
    }

    /**
     * Set the FlagsParameterization to be used during this test. This cannot be used to override a
     * previous call, and cannot be called once the rule has been evaluated.
     */
    public void setFlagsParameterization(@Nonnull FlagsParameterization flagsParameterization) {
        Objects.requireNonNull(flagsParameterization, "FlagsParameterization cannot be cleared");
        if (mFlagsParameterization != null) {
            throw new AssertionError("FlagsParameterization cannot be overridden");
        }
        if (mIsRuleEvaluating) {
            throw new AssertionError("Cannot set FlagsParameterization once the rule is running");
        }
        ensureFlagsAreUnset();
        mFlagsParameterization = flagsParameterization;
        mLockedFlagNames.addAll(flagsParameterization.mOverrides.keySet());
    }

    /**
     * Enables the given flags.
     *
     * @param fullFlagNames The name of the flags in the flag class with the format
     *     {packageName}.{flagName}
     */
    public void enableFlags(String... fullFlagNames) {
        for (String fullFlagName : fullFlagNames) {
            if (mLockedFlagNames.contains(fullFlagName)) {
                throw new FlagSetException(fullFlagName, "Not allowed to change locked flags");
            }
            setFlagValue(fullFlagName, true);
        }
    }

    /**
     * Disables the given flags.
     *
     * @param fullFlagNames The name of the flags in the flag class with the format
     *     {packageName}.{flagName}
     */
    public void disableFlags(String... fullFlagNames) {
        for (String fullFlagName : fullFlagNames) {
            if (mLockedFlagNames.contains(fullFlagName)) {
                throw new FlagSetException(fullFlagName, "Not allowed to change locked flags");
            }
            setFlagValue(fullFlagName, false);
        }
    }

    /**
     * Returns a FeatureFlags used by SetFlagsRule of given FeatureFlags
     *
     * @param featureFlagsClass The class of FeatureFlags. The interface of FakeFeatureFlagsImpl
     * @return A FakeFeatureFlagsImpl in type of FeatureFlags
     */
    public <T> T getFakeFeatureFlags(Class<T> featureFlagsClass) {
        if (!featureFlagsClass.isInterface()
                || !featureFlagsClass.getSimpleName().equals(FEATURE_FLAGS_CLASS_NAME)) {
            throw new IllegalArgumentException(
                    String.format(
                            "%s is not a FeatureFlags. " + "Please pass in FeatureFlags interface",
                            featureFlagsClass));
        }

        String packageName = featureFlagsClass.getPackageName();
        String flagsClassName = String.format("%s.%s", packageName, FLAGS_CLASS_NAME);
        Class<?> flagsClass = null;

        try {
            flagsClass = Class.forName(flagsClassName);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(
                    String.format("Failed to load class %s.", flagsClassName));
        }

        Object fakeFlagsImplInstance = getOrCreateFakeFlagsImp(flagsClass);

        return featureFlagsClass.cast(fakeFlagsImplInstance);
    }

    private void ensureFlagsAreUnset() {
        if (!mFlagsClassToFakeFlagsImpl.isEmpty()) {
            throw new AssertionError("Some flags were set before the rule was initialized");
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable throwable = null;
                try {
                    AnnotationsRetriever.FlagAnnotations flagAnnotations =
                            AnnotationsRetriever.getFlagAnnotations(description);
                    assertAnnotationsMatchParameterization(flagAnnotations, mFlagsParameterization);
                    flagAnnotations.assumeAllSetFlagsMatchParameterization(mFlagsParameterization);
                    if (mFlagsParameterization != null) {
                        ensureFlagsAreUnset();
                        for (Map.Entry<String, Boolean> pair :
                                mFlagsParameterization.mOverrides.entrySet()) {
                            setFlagValue(pair.getKey(), pair.getValue());
                        }
                    }
                    for (Map.Entry<String, Boolean> pair :
                            flagAnnotations.mSetFlagValues.entrySet()) {
                        setFlagValue(pair.getKey(), pair.getValue());
                    }
                    mLockedFlagNames.addAll(flagAnnotations.mRequiredFlagValues.keySet());
                    mLockedFlagNames.addAll(flagAnnotations.mSetFlagValues.keySet());
                    mIsRuleEvaluating = true;
                    base.evaluate();
                } catch (Throwable t) {
                    throwable = t;
                } finally {
                    mIsRuleEvaluating = false;
                    try {
                        resetFlags();
                    } catch (Throwable t) {
                        if (throwable != null) {
                            t.addSuppressed(throwable);
                        }
                        throwable = t;
                    }
                }
                if (throwable != null) throw throwable;
            }
        };
    }

    private static void assertAnnotationsMatchParameterization(
            AnnotationsRetriever.FlagAnnotations flagAnnotations,
            FlagsParameterization parameterization) {
        if (parameterization == null) return;
        Set<String> parameterizedFlags = parameterization.mOverrides.keySet();
        Set<String> requiredFlags = flagAnnotations.mRequiredFlagValues.keySet();
        // Assert that NO Annotation-Required flag is in the parameterization
        Set<String> parameterizedAndRequiredFlags =
                Sets.intersection(parameterizedFlags, requiredFlags);
        if (!parameterizedAndRequiredFlags.isEmpty()) {
            throw new AssertionError(
                    "The following flags have required values (per @RequiresFlagsEnabled or"
                            + " @RequiresFlagsDisabled) but they are part of the"
                            + " FlagParameterization: "
                            + parameterizedAndRequiredFlags);
        }
    }

    private void setFlagValue(String fullFlagName, boolean value) {
        if (!fullFlagName.contains(".")) {
            throw new FlagSetException(
                    fullFlagName, "Flag name is not the expected format {packgeName}.{flagName}.");
        }
        Flag flag = Flag.createFlag(fullFlagName);
        Object fakeFlagsImplInstance = null;

        Class<?> flagsClass = getFlagClassFromFlag(flag);
        fakeFlagsImplInstance = getOrCreateFakeFlagsImp(flagsClass);

        Map<Flag, Boolean> flagToValue =
                mFlagsClassToFlagDefaultMap.getOrDefault(flagsClass, new HashMap<>());
        if (flagToValue.isEmpty()) {
            // Replace FeatureFlags in Flags class with FakeFeatureFlagsImpl
            replaceFlagsImpl(flagsClass, fakeFlagsImplInstance);
            mFlagsClassToFlagDefaultMap.put(flagsClass, flagToValue);
        }

        // Store a copy of the original value so that it can be restored later
        if (!flagToValue.containsKey(flag)) {
            flagToValue.put(
                    flag, mIsInitWithDefault ? getFlagValue(fakeFlagsImplInstance, flag) : null);
        }

        // Set desired flag value in the FakeFeatureFlagsImpl
        setFlagValue(fakeFlagsImplInstance, flag, value);
    }

    private void populateFakeFlagsImplWithDefault(Class<?> flagClass) {
        Object fakeFlagsImpl = mFlagsClassToFakeFlagsImpl.get(flagClass);
        Object realFlagsImpl = mFlagsClassToRealFlagsImpl.get(flagClass);
        if (fakeFlagsImpl == null || realFlagsImpl == null) {
            throw new FlagSetException(
                    flagClass.getName(),
                    String.format(
                            "Failed populate %s with default value from %s",
                            fakeFlagsImpl.getClass().getName(),
                            realFlagsImpl.getClass().getName()));
        }
        try {
            for (Field field : flagClass.getFields()) {
                String fullFlagName = (String) field.get(null);
                Flag flag = Flag.createFlag(fullFlagName);
                boolean value = getFlagValue(realFlagsImpl, flag);

                setFlagValue(fakeFlagsImpl, flag, value);
            }
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(
                    String.format("Failed to get field in class %s", flagClass.getName()), e);
        }
    }

    private Class<?> getFlagClassFromFlag(Flag flag) {
        String className = flag.flagsClassName();
        Class<?> flagsClass = null;
        try {
            flagsClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new FlagSetException(
                    flag.fullFlagName(),
                    String.format(
                            "Can not load the Flags class %s to set its values. Please check the "
                                    + "flag name and ensure that the aconfig auto generated "
                                    + "library is in the dependency.",
                            className),
                    e);
        }
        return flagsClass;
    }

    private boolean getFlagValue(Object featureFlagsImpl, Flag flag) {
        // Must be consistent with method name in aconfig auto generated code.
        String methodName =
                CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, flag.simpleFlagName());
        String fullFlagName = flag.fullFlagName();

        try {
            Object result =
                    featureFlagsImpl.getClass().getMethod(methodName).invoke(featureFlagsImpl);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            throw new FlagReadException(
                    fullFlagName,
                    String.format(
                            "Flag type is %s, not boolean", result.getClass().getSimpleName()));
        } catch (NoSuchMethodException e) {
            throw new FlagReadException(
                    fullFlagName,
                    String.format(
                            "No method %s in the Flags class %s to read the flag value. Please"
                                    + " check the flag name.",
                            methodName, featureFlagsImpl.getClass().getName()),
                    e);
        } catch (ReflectiveOperationException e) {
            throw new FlagReadException(
                    fullFlagName,
                    String.format(
                            "Fail to get value of flag %s from instance %s",
                            fullFlagName, featureFlagsImpl.getClass().getName()),
                    e);
        }
    }

    private void setFlagValue(Object featureFlagsImpl, Flag flag, boolean value) {
        String fullFlagName = flag.fullFlagName();
        try {
            featureFlagsImpl
                    .getClass()
                    .getMethod(SET_FLAG_METHOD_NAME, String.class, boolean.class)
                    .invoke(featureFlagsImpl, fullFlagName, value);
        } catch (NoSuchMethodException e) {
            throw new FlagSetException(
                    fullFlagName,
                    String.format(
                            "Flag implementation %s is not fake implementation",
                            featureFlagsImpl.getClass().getName()),
                    e);
        } catch (ReflectiveOperationException e) {
            throw new FlagSetException(fullFlagName, e);
        }
    }

    @Nonnull
    private Object getOrCreateFakeFlagsImp(Class<?> flagsClass) {
        Object fakeFlagsImplInstance = mFlagsClassToFakeFlagsImpl.get(flagsClass);
        if (fakeFlagsImplInstance != null) {
            return fakeFlagsImplInstance;
        }

        String packageName = flagsClass.getPackageName();
        String className = String.format("%s.%s", packageName, FAKE_FEATURE_FLAGS_IMPL_CLASS_NAME);

        try {
            Class<?> flagImplClass = Class.forName(className);
            fakeFlagsImplInstance = flagImplClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot create FakeFeatureFlagsImpl in Flags class %s.",
                            flagsClass.getName()),
                    e);
        }

        mFlagsClassToFakeFlagsImpl.put(flagsClass, fakeFlagsImplInstance);

        // Store the real FeatureFlagsImpl instance
        Field featureFlagsField = getFeatureFlagsField(flagsClass);
        featureFlagsField.setAccessible(true);
        try {
            mFlagsClassToRealFlagsImpl.put(flagsClass, featureFlagsField.get(null));
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot set FakeFeatureFlagsImpl in Flags class %s.",
                            flagsClass.getName()),
                    e);
        }

        if (mIsInitWithDefault) {
            populateFakeFlagsImplWithDefault(flagsClass);
        }

        return fakeFlagsImplInstance;
    }

    private void replaceFlagsImpl(Class<?> flagsClass, Object flagsImplInstance) {
        Field featureFlagsField = getFeatureFlagsField(flagsClass);
        featureFlagsField.setAccessible(true);
        try {
            featureFlagsField.set(null, flagsImplInstance);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot replace FeatureFlagsImpl to %s.",
                            flagsImplInstance.getClass().getName()),
                    e);
        }
    }

    private Field getFeatureFlagsField(Class<?> flagsClass) {
        Field featureFlagsField = null;
        try {
            featureFlagsField = flagsClass.getDeclaredField(FEATURE_FLAGS_FIELD_NAME);
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Cannot store FeatureFlagsImpl in Flag %s.", flagsClass.getName()),
                    e);
        }
        return featureFlagsField;
    }

    private void resetFlags() {
        String flagsClassName = null;
        try {
            for (Class<?> flagsClass : mFlagsClassToFlagDefaultMap.keySet()) {
                Map<Flag, Boolean> flagToValue = mFlagsClassToFlagDefaultMap.get(flagsClass);
                flagsClassName = flagsClass.getName();
                Object fakeFlagsImplInstance = mFlagsClassToFakeFlagsImpl.get(flagsClass);
                Object flagsImplInstance = mFlagsClassToRealFlagsImpl.get(flagsClass);
                // Replace FeatureFlags in Flags class with real FeatureFlagsImpl
                replaceFlagsImpl(flagsClass, flagsImplInstance);
                if (mIsInitWithDefault) {
                    for (Map.Entry<Flag, Boolean> entry : flagToValue.entrySet()) {
                        setFlagValue(fakeFlagsImplInstance, entry.getKey(), entry.getValue());
                    }
                } else {
                    fakeFlagsImplInstance
                            .getClass()
                            .getMethod(RESET_ALL_METHOD_NAME)
                            .invoke(fakeFlagsImplInstance);
                }
            }
            mFlagsClassToFlagDefaultMap.clear();
        } catch (Exception e) {
            throw new FlagSetException(flagsClassName, e);
        }
    }
}
