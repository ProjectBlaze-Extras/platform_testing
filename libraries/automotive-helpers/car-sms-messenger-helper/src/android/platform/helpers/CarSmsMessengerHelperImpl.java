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

import androidx.test.uiautomator.BySelector;

/** CarSmsMessengerHelperImpl class for Android Auto platform functional tests */
public class CarSmsMessengerHelperImpl extends AbstractStandardAppHelper
        implements IAutoCarSmsMessengerHelper {

    public CarSmsMessengerHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return getPackageFromConfig(AutomotiveConfigConstants.SMS_PACKAGE);
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

    /** {@inheritDoc} */
    @Override
    public void open() {
        getSpectatioUiUtil().pressHome();
        getSpectatioUiUtil().wait1Second();
        getSpectatioUiUtil()
                .executeShellCommand(
                        getCommandFromConfig(AutomotiveConfigConstants.OPEN_SMS_ACTIVITY_COMMAND));
        getSpectatioUiUtil().wait1Second();
    }

    @Override
    public boolean isSmsBluetoothErrorDisplayed() {
        BySelector bluetoothErrorSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.SMS_BLUETOOTH_ERROR);
        return getSpectatioUiUtil().hasUiElement(bluetoothErrorSelector);
    }
}
