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

import android.app.Instrumentation;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;

/** Bluetooth Setting Helper class for Android Auto platform functional tests */
public class SettingsBluetoothHelperImpl extends AbstractStandardAppHelper
        implements IAutoBluetoothSettingsHelper {

    private static final String LOG_TAG = SettingHelperImpl.class.getSimpleName();

    public SettingsBluetoothHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public void pressBluetoothToggleOnDevice(String deviceName) {

        /**
         * Note: this method currently does not select strictly within the passed-in device name.
         */
        BySelector toggleSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.TOGGLE_DEVICE_BLUETOOTH);

        UiObject2 toggleButton = getSpectatioUiUtil().findUiObject(toggleSelector);

        getSpectatioUiUtil()
                .validateUiObject(toggleButton, AutomotiveConfigConstants.TOGGLE_DEVICE_BLUETOOTH);
        getSpectatioUiUtil().clickAndWait(toggleButton);
    }

    /** {@inheritDoc} */
    @Override
    public void pressDevice(String deviceName) {

        BySelector nameField = By.text(deviceName);
        BySelector clickable = By.hasDescendant(By.hasDescendant(nameField));
        UiObject2 clickableDevice = getSpectatioUiUtil().findUiObject(clickable);
        getSpectatioUiUtil()
                .validateUiObject(clickableDevice, "Viewgroup ancestor of  " + deviceName);
        getSpectatioUiUtil().clickAndWait(clickableDevice);
    }

    /** {@inheritDoc} */
    @Override
    public String getDeviceSummary() {

        BySelector statusSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.DEVICE_HEADER_SUMMARY);
        UiObject2 statusField = getSpectatioUiUtil().findUiObject(statusSelector);
        getSpectatioUiUtil()
                .validateUiObject(statusField, AutomotiveConfigConstants.DEVICE_HEADER_SUMMARY);
        return statusField.getText();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConnected() {
        // Recall that a connected device will show 'Disconnected'
        return !getDeviceSummary().contains("Disconnected");
    }

    /** {@inheritDoc} */
    @Override
    public void pressForget() {
        BySelector forgetSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.DEVICE_FORGET_BUTTON);
        UiObject2 forgetButton = getSpectatioUiUtil().findUiObject(forgetSelector);
        getSpectatioUiUtil()
                .validateUiObject(forgetButton, AutomotiveConfigConstants.DEVICE_FORGET_BUTTON);
        getSpectatioUiUtil().clickAndWait(forgetButton);
    }

    /** {@inheritDoc} */
    @Override
    public void pressConnectionToggle() {
        BySelector connectionSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.DEVICE_CONNECTION_BUTTON);
        UiObject2 connectionButton = getSpectatioUiUtil().findUiObject(connectionSelector);
        getSpectatioUiUtil()
                .validateUiObject(
                        connectionButton, AutomotiveConfigConstants.DEVICE_CONNECTION_BUTTON);
        getSpectatioUiUtil().clickAndWait(connectionButton);
    }

    /** {@inheritDoc} */
    @Override
    public void goBackToBluetoothSettings() {
        BySelector connectionSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.DEVICE_CONNECTION_BACK_BUTTON);
        UiObject2 connectionButton = getSpectatioUiUtil().findUiObject(connectionSelector);
        getSpectatioUiUtil()
                .validateUiObject(
                        connectionButton, AutomotiveConfigConstants.DEVICE_CONNECTION_BACK_BUTTON);
        getSpectatioUiUtil().clickAndWait(connectionButton);
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return getPackageFromConfig(AutomotiveConfigConstants.SETTINGS_PACKAGE);
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void dismissInitialDialogs() {
        // Nothing to dismiss
    }
}
