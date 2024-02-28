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

from bluetooth_test import bluetooth_base_test

from mobly import asserts
from mobly import test_runner


class DeviceNotPairedTest(bluetooth_base_test.BluetoothBaseTest):

    def setup_test(self):
        """Enable and disable BT on Head unit"""
        self.discoverer.mbs.btEnable()
        self.discoverer.mbs.btDisable()

    def test_device_not_paired(self):
        """Tests lunch Bluetooth Audio app and verify <Bluetooth Disconnected> displayed."""
        self.call_utils.open_bluetooth_media_app()
        asserts.assert_true(self.call_utils.is_bluetooth_audio_disconnected_label_visible(),
                            '<Bluetooth Audio disconnected> label should be present')


if __name__ == '__main__':
    # Pass test arguments after '--' to the test runner.
    # Needed for Mobly Test Runner
    if '--' in sys.argv:
        index = sys.argv.index('--')
        sys.argv = sys.argv[:1] + sys.argv[index + 1:]
    test_runner.main()
