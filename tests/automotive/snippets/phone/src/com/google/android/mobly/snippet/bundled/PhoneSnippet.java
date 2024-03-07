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

import android.content.Context;
import android.os.Build;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoDialHelper;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;

/** Snippet class for Phone RPCs */
public class PhoneSnippet implements Snippet {

    private final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    private final HelperAccessor<IAutoDialHelper> mDialerHelper =
            new HelperAccessor<>(IAutoDialHelper.class);

    public PhoneSnippet() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        mTelephonyManager = context.getSystemService(TelephonyManager.class);

        mSubscriptionManager =
                (SubscriptionManager)
                        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    /** Gets Phonenumber of the test device */
    @Rpc(description = "Returns the phone Number")
    // getLine1Number() has been deprecated from api 33
    public String getPhoneNumber() {
        if (Build.VERSION.SDK_INT >= 33) {
            return mSubscriptionManager.getPhoneNumber(
                    mSubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
        } else {
            return mTelephonyManager.getLine1Number();
        }
    }

    /** Press the device prompt on screen */
    @Rpc(description = "Press 'Device' on a prompt, if present.")
    public void pressDevice() {
        mDialerHelper.get().pressDeviceOnPrompt();
    }

    @Override
    public void shutdown() {}
}
