"""
  Copyright (C) 2023 The Android Open Source Project

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.



  Test Steps:
  (0. Flash device)
  1. Verify by default BT should be ON always
  2. BluetoothManagerService: Startup: Bluetooth persisted state is ON

"""

import sys
import logging
import pprint

from mobly import asserts
from mobly import base_test
from mobly import test_runner
from mobly.controllers import android_device

from mbs_utils import constants
from mbs_utils import spectatio_utils
from mbs_utils import bt_utils


class BluetoothDefaultStateTest(base_test.BaseTestClass):

    def setup_class(self):
        # Registering android_device controller module, and declaring that the test
        # requires at least two Android devices.
        self.ads = self.register_controller(android_device, min_number=2)

        # Even in a default state test, tags should be set to make debugging easier.
        self.discoverer = android_device.get_device(
            self.ads, label='auto')
        self.discoverer.debug_tag = 'discoverer'
        self.discoverer.load_snippet('mbs', android_device.MBS_PACKAGE)


    def setup_test(self):
        # Default state test requires no setup.
        pass

    def test_device_connectivity_in_level_two(self):
        # Confirm that the bluetooth state is ON
        asserts.assert_true(
            self.discoverer.mbs.btIsEnabled(),
            "Expected bluetooth to be enabled by default, but it was not.")

    def teardown_test(self):
        # Default state test should still disable bluetooth after checking default state.
        self.discoverer.mbs.btDisable()



if __name__ == '__main__':
    # Take test args
    test_runner.main()