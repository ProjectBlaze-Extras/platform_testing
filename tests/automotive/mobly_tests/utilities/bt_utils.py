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
import pprint
import time

from mbs_utils import constants
from mobly import asserts
from mobly.controllers import android_device

# Number of seconds for the target to stay discoverable on Bluetooth.
DISCOVERABLE_TIME = 60
TIME_FOR_PROMPT_TO_LOAD = 2
class BTUtils:
    """A utility that provides access to Bluetooth connectivity controls."""

    def __init__(self, discoverer, target):
        self.discoverer = discoverer
        self.target = target

    def get_info_from_devices(self, discovered_devices):
        discovered_names = [device['Name'] for device in discovered_devices]
        discovered_addresses = [device['Address'] for device in discovered_devices]
        return discovered_names, discovered_addresses

    def discover_secondary_from_primary(self):
        target_name = self.target.mbs.btGetName()
        self.target.log.info('Become discoverable with name "%s" for %ds.',
                             target_name, DISCOVERABLE_TIME)
        self.target.mbs.btBecomeDiscoverable(DISCOVERABLE_TIME)
        self.discoverer.log.info('Looking for Bluetooth devices.')
        discovered_devices = self.discoverer.mbs.btDiscoverAndGetResults()
        self.discoverer.log.debug('Found Bluetooth devices: %s',
                                  pprint.pformat(discovered_devices, indent=2))
        discovered_names, _ = self.get_info_from_devices(discovered_devices)
        logging.info('Verifying the target is discovered by the discoverer.')
        asserts.assert_true(
            target_name in discovered_names,
            'Failed to discover the target device %s over Bluetooth.' %
            target_name)

    def pair_primary_to_secondary(self):
        """Enable discovery on the target so the discoverer can find it."""
        # Turn bluetooth on in both machines
        logging.info('Enabling Bluetooth on both devices')
        self.discoverer.mbs.btEnable()
        self.target.mbs.btEnable()
        logging.info('Setting devices to be discoverable')
        self.target.mbs.btBecomeDiscoverable(DISCOVERABLE_TIME)
        self.target.mbs.btStartAutoAcceptIncomingPairRequest()
        target_address = self.target.mbs.btGetAddress()
        logging.info('Scanning for discoverable devices')
        # Discovery of target device is tried 5 times.
        discovered_devices = self.discoverer.mbs.btDiscoverAndGetResults()
        self.discoverer.mbs.btPairDevice(target_address)
        logging.info('Allowing time for contacts to sync.')
        time.sleep(constants.SYNC_WAIT_TIME)
        paired_devices = self.discoverer.mbs.btGetPairedDevices()
        _, paired_addresses = self.get_info_from_devices(paired_devices)
        asserts.assert_true(
            target_address in paired_addresses,
            'Failed to pair the target device %s over Bluetooth.' %
            target_address)

    def press_allow_on_device(self):
        """ Repeatedly presses "Allow" on prompts until no more prompts appear"""
        logging.info('Attempting to press ALLOW')
        while (self.target.mbs.btPressAllow()):
            logging.info('ALLOW pressed!')
            time.sleep(TIME_FOR_PROMPT_TO_LOAD)

