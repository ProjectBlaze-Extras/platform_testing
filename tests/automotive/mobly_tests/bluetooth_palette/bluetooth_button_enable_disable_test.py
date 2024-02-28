#  Copyright (C) 2023 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.


import sys
import logging
import pprint


from bluetooth_test import bluetooth_base_test
from mobly import asserts
from mobly import test_runner
from mobly.controllers import android_device

from mbs_utils import constants
from mbs_utils import spectatio_utils
from mbs_utils import bt_utils


class BluetoothPalette(bluetooth_base_test.BluetoothBaseTest):
  """Enable and Disable Bluetooth from Bluetooth Palette."""

  def setup_test(self):
    """Setup steps before any test is executed."""
    # Pair the devices
    self.bt_utils.pair_primary_to_secondary();

  def test_enable_disable_bluetooth_button(self):
    """Tests enable and disable functionality of bluetooth."""
    self.call_utils.open_bluetooth_palette()
    self.call_utils.wait_with_log(
    constants.DEFAULT_WAIT_TIME_FIVE_SECS
    )
    asserts.assert_true(self.call_utils.is_bluetooth_connected(),'Bluetooth Connected')
    self.call_utils.click_bluetooth_button()
    self.call_utils.wait_with_log(
    constants.DEFAULT_WAIT_TIME_FIVE_SECS
    )
    asserts.assert_false(self.call_utils.is_bluetooth_connected(),'Bluetooth Disconnected')



if __name__ == '__main__':
    # Pass test arguments after '--' to the test runner.
    # Needed for Mobly Test Runner
    if '--' in sys.argv:
        index = sys.argv.index('--')
        sys.argv = sys.argv[:1] + sys.argv[index + 1:]
    test_runner.main()
