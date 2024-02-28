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

package com.google.android.mobly.snippet.bundled;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoStatusBarHelper;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;

/** Snippet class for exposing Status Bar App APIs. */
public class StatusBarSnippet implements Snippet {

    private final HelperAccessor<IAutoStatusBarHelper> mStatusBarHelper;

    public StatusBarSnippet() {
        mStatusBarHelper = new HelperAccessor<>(IAutoStatusBarHelper.class);
    }

    @Rpc(description = "Verify Disabled Bluetooth Profile")
    public boolean verifyDisabledBluetoothProfile() {
        return mStatusBarHelper.get().verifyDisabledBluetoothProfile();
    }

    @Rpc(description = "Verify Disabled Phone Profile")
    public boolean verifyDisabledPhoneProfile() {
        return mStatusBarHelper.get().verifyDisabledPhoneProfile();
    }

    @Rpc(description = "Verify Disabled Media Profile")
    public boolean verifyDisabledMediaProfile() {
        return mStatusBarHelper.get().verifyDisabledMediaProfile();
    }

    @Rpc(description = "is Mobile Connected")
    public boolean isBluetoothConnectedToMobile() {
        return mStatusBarHelper.get().isBluetoothConnectedToMobile();
    }

    @Rpc(description = "is Mobile Disconnected")
    public boolean isBluetoothDisconnected() {
        return mStatusBarHelper.get().isBluetoothDisconnected();
    }

    @Rpc(description = "Open Bluetooth Palette")
    public void openBluetoothPalette() {
        mStatusBarHelper.get().openBluetoothPalette();
    }

    @Rpc(description = "Click Bluetooth Button on the status bar")
    public void clickBluetoothButton() {
        mStatusBarHelper.get().clickBluetoothButton();
    }

    @Rpc(description = "is Bluetooth Connected")
    public boolean isBluetoothConnected() {
        return mStatusBarHelper.get().isBluetoothConnected();
    }

    @Rpc(description = "Press the Home icon on the status bar")
    public void pressHome() {
        mStatusBarHelper.get().open();
    }

    @Rpc(description = "Verify Bluetooth")
    public boolean verifyBluetooth() {
        return mStatusBarHelper.get().verifyBluetooth();
    }

    @Rpc(description = "Verify Phone")
    public boolean verifyPhone() {
        return mStatusBarHelper.get().verifyPhone();
    }

    @Rpc(description = "Verify Media")
    public boolean verifyMedia() {
        return mStatusBarHelper.get().verifyMedia();
    }

    @Rpc(description = "Verify Device Name")
    public boolean verifyDeviceName() {
        return mStatusBarHelper.get().verifyDeviceName();
    }

    @Override
    public void shutdown() {}
}
