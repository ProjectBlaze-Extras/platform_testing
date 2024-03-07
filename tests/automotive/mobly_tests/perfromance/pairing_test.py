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

import logging
import time
from absl import flags

from bluetooth_test import bluetooth_base_test
from utilities.main_utils import common_main
from utilities.crystalball_metrics_utils import export_to_crystalball

_BT_PAIRING_ITERATIONS = flags.DEFINE_integer('iterations', 10,
                                              'Number of iterations to pair/unpair')

_BUFFER_TIME_BETWEEN_ITERATIONS_S = flags.DEFINE_integer('buffer-time-between-iterations', 2,
                                                         'Time to wait in seconds between test iterations to reduce stress on bt stack')


class BTPerformancePairingTest(bluetooth_base_test.BluetoothBaseTest):
    """Test Class for Bluetooth Pairing Test."""

    def test_pairing(self):
        """Test for pairing/unpairing a HU with a bluetooth device"""
        pairing_success_count = 0
        for i in range(1, _BT_PAIRING_ITERATIONS.value + 1):
            logging.info(f'Pairing iteration {i}')
            try:
                self.bt_utils.pair_primary_to_secondary()
                pairing_success_count += 1
            except:
                logging.error(f'Failed to pair devices on iteration {i}')
            self.bt_utils.unpair()
            time.sleep(_BUFFER_TIME_BETWEEN_ITERATIONS_S.value)
        success_rate = pairing_success_count / _BT_PAIRING_ITERATIONS.value
        metrics = {'success_rate': success_rate}
        export_to_crystalball(metrics, self.log_path, self.current_test_info.name)


if __name__ == '__main__':
    common_main()
