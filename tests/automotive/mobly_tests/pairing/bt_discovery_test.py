"""
Pairing Test
"""

import sys
import logging
import pprint

from mobly import asserts
from mobly import base_test
from mobly import test_runner
from mobly.controllers import android_device
from bluetooth_test import bluetooth_base_test

# Number of seconds for the target to stay discoverable on Bluetooth.
DISCOVERABLE_TIME = 120

class MultiDeviceTest(bluetooth_base_test.BluetoothBaseTest):

    def setup_test(self):
        super().setup_test()
        # Set Bluetooth name on target device.
        self.target.mbs.btSetName('LookForMe!')

    def test_bluetooth_discovery(self):
        self.bt_utils.discover_secondary_from_primary()

    def test_bluetooth_pair(self):
        self.bt_utils.pair_primary_to_secondary()

if __name__ == '__main__':
    # Pass test arguments after '--' to the test runner.
    # Needed for Mobly Test Runner
    if '--' in sys.argv:
        index = sys.argv.index('--')
        sys.argv = sys.argv[:1] + sys.argv[index + 1:]
    test_runner.main()
