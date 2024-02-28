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
package android.platform.helpers.uinput

/**
 * Class to represent a stylus as a [UInputDevice]. This can then be used to register a stylus using
 * [android.platform.test.rule.InputDeviceRule].
 *
 * @param supportedKeys should be keycodes obtained from the linux event code set. See
 *   https://cs.android.com/android/kernel/superproject/+/common-android-mainline:common/include/uapi/linux/input-event-codes.h
 */
class UInputStylus(
    val inputDeviceId: Int,
    val displayWidth: Int,
    val displayHeight: Int,
    override val vendorId: Int = VENDOR_ID,
    override val productId: Int = PRODUCT_ID,
    override val name: String = "Test capacitive stylus with buttons",
    override val supportedKeys: Array<Int> = SUPPORTED_KEYBITS,
) : UInputDevice() {
    override val bus = "bluetooth"

    override fun getRegisterCommand() =
        """{
      "id": $inputDeviceId,
      "type": "uinput",
      "command": "register",
      "name": "$name",
      "vid": $vendorId,
      "pid": $productId,
      "bus": "$bus",
      "configuration": [
        {"type": 100, "data": ${SUPPORTED_ENVBITS.contentToString()}},  // UI_SET_EVBIT
        {"type": 101, "data": ${SUPPORTED_KEYBITS.contentToString()}},  // UI_SET_KEYBIT
        {"type": 103, "data": ${SUPPORTED_ABSBITS.contentToString()}},  // UI_SET_ABSBIT
        {"type": 110, "data": [1]}  // UI_SET_PROPBIT : INPUT_PROP_DIRECT
      ],
      "abs_info": [
        {"code":0x00, "info": {       // ABS_X
          "value": 0,
          "minimum": 0,
          "maximum": 3199, // Display width * 2 - 1. Hardcoded with values from recording device.
          "fuzz": 0,
          "flat": 0,
          "resolution": 0
        }},
        {"code":0x01, "info": {       // ABS_Y
          "value": 0,
          "minimum": 0,
          "maximum": 5119, // Display height * 2 - 1. Hardcoded with values from recording device.
          "fuzz": 0,
          "flat": 0,
          "resolution": 0
        }},
        {"code":0x18, "info": {       // ABS_PRESSURE
          "value": 0,
          "minimum": 0,
          "maximum": 4095,
          "fuzz": 0,
          "flat": 0,
          "resolution": 0
        }},
        {"code":0x1a, "info": {       // ABS_TILT_X
          "value": 0,
          "minimum": -60,
          "maximum": 60,
          "fuzz": 0,
          "flat": 0,
          "resolution": 0
        }},
        {"code":0x1b, "info": {       // ABS_TILT_Y
          "value": 0,
          "minimum": -60,
          "maximum": 60,
          "fuzz": 0,
          "flat": 0,
          "resolution": 0
        }}
      ]
    }

    {
      "id": 1,
      "command": "delay",
      "duration": 3000
    }

    // Sends an event which tells the device that this is a stylus device not a finger.
    {
      "id": $inputDeviceId,
      "command": "inject",
      "events": [
        1,    // EV_KEY
        320,  // BTN_TOOL_PEN
        1
      ]
    }
    """

    /** Sends the STYLUS_BUTTON_TAIL event. */
    fun sendTailButtonClickEvent(eventInjector: EventInjector) {
        // KEY_JOURNAL is remapped to STYLUS_BUTTON_TAIL.
        eventInjector.sendKeyEvent(inputDeviceId, KEY_JOURNAL)
    }

    /** Sends the events contained within the specified file. */
    fun sendEventsFromInputFile(eventInjector: EventInjector, assetsFilePath: String) {
        eventInjector.sendEventsFromInputFile(inputDeviceId, assetsFilePath)
    }

    private companion object {
        // EV_KEY
        val SUPPORTED_ENVBITS =
            arrayOf(
                /* EV_KEY */ 1,
                /* EV_ABS */ 3,
            )
        // UI_SET_ABSBIT
        val SUPPORTED_ABSBITS =
            arrayOf(
                /* ABS_X */ 0,
                /* ABS_Y */ 1,
                /* ABS_PRESSURE */ 24,
                /* ABS_TILT_X */ 26,
                /* ABS_TILT_Y */ 27,
            )
        const val KEY_JOURNAL = 578
        // UI_SET_KEYBIT
        val SUPPORTED_KEYBITS =
            arrayOf(
                /* BTN_TOOL_PEN */ 320,
                /* BTN_TOUCH */ 330,
                /* BTN_STYLUS */ 331,
                /* BTN_STYLUS2 */ 332,
                KEY_JOURNAL,
            )
        // Using the below combination of product and vendor ID allows us to remap KEY_JOURNAL to
        // STYLUS_BUTTON_TAIL. This is because STYLUS_BUTTON_TAIL is not a proper Linux input event
        // code yet and we remap the buttons for generic android stylus which is represented by the
        // below combination of IDs.
        const val VENDOR_ID = 0x18d1
        const val PRODUCT_ID = 0x4f80
    }
}
