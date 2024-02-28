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


from mobly.controllers import android_device
from mbs_utils import constants

""" This exception may be expanded in the future to provide better error discoverability."""
class CallUtilsError(Exception):
    pass

class CallUtils:
    """Calling sequence utility for BT calling test using Spectatio UI APIs.

    This class provides functions that execute generic call sequences. Specific
    methods
    (e.g., verify_precall_state) are left to that implementation, and thus the
    utilities housed here are meant to describe generic sequences of actions.

    """

    def __init__(self, device):
        self.device = device

    def dial_a_number(self, callee_number):
        """ Dial phone number """
        logging.info('Dial phone number <%s>', callee_number)
        self.device.mbs.dialANumber(callee_number)


    def end_call(self):
        """  End the call. Throws an error if non call is currently ongoing. """
        logging.info('End the call')
        self.device.mbs.endCall()


    def execute_shell_on_device(self, device_target, shell_command):
        """Execute any shell command on any device"""
        logging.info(
            'Executing shell command: <%s> on device <%s>',
            shell_command,
            device_target.serial,
        )
        device_target.adb.shell(shell_command)


    def get_dialing_number(self):
        """ Get dialing phone number"""
        return self.device.mbs.getDialedNumber()

    def get_home_address_from_details(self):
        """Return the home address of the contact whose details are currently being displayed"""
        return self.device.mbs.getHomeAddress()

    def get_device_summary(self):
        """Assumes the device summary page is open."""
        return self.device.mbs.getDeviceSummary()

    def import_contacts_from_vcf_file(self, device_target):
        """ Importing contacts from VCF file"""
        logging.info('Importing contacts from VCF file to device Contacts')
        self.execute_shell_on_device(
            device_target,
            constants.IMPOST_CONTACTS_SHELL_COMAND,
        )

    def make_call(self):
        """ Make call"""
        logging.info('Make a call')
        self.device.mbs.makeCall()

    def open_call_history(self):
        """ Open call history """
        logging.info('Open call history')
        self.device.mbs.openCallHistory()

    def open_contacts(self):
        """Open contacts"""
        logging.info('Opening contacts')
        self.device.mbs.openContacts()

    def open_phone_app(self):
        logging.info('Opening phone app')
        self.device.mbs.openPhoneApp()

    def open_first_contact_details(self):
        """Open the contact details page for the first contact visible in the contact list.
        Assumes we are on the contacts page. """
        logging.info('Getting details for first contact on the page')
        self.device.mbs.openFirstContactDetails()

    def open_bluetooth_settings(self):
        """Assumes we are on the home screen.
        Navigate to the Bluetooth setting page"""
        logging.info("Pressing the bluetooth indicator icon in the status bar.")
        self.device.mbs.openBluetoothSettings()

    def press_bluetooth_toggle_on_device(self, device_name):
        logging.info('Attempting to press the bluetooth toggle on device: ' + device_name)
        self.device.mbs.pressBluetoothToggleOnDevice(device_name)

    def press_device_entry_on_list_of_paired_devices(self, device_name):
        logging.info('Attempting to press the device entry on device: ' + device_name)
        self.device.mbs.pressDeviceName(device_name)

    def device_displays_connected(self):
        """Assumes the device bluetooth connection settings page is open"""
        logging.info('Checking whether device is connected.')
        self.device.mbs.deviceIsConnected()

    def press_forget(self):
        """Assumes the device bluetooth connection settings page is open"""
        logging.info('Attempting to press \'Forget\'')
        self.device.mbs.pressForget()


    def press_home_screen_on_status_bar(self):
        """Presses the Home screen button on the status bar
        (to return the device to the home screen."""
        logging.info("Pressing home screen button")
        self.device.mbs.pressHomeScreen()

    def press_home(self):
        """ Press the Home button to go back to the home page."""
        logging.info("Pressing HOME ")
        self.device.mbs.pressHome();

    def press_enter_on_device(self, device_target):
        """Press ENTER on device"""
        logging.info('Pressing ENTER on device: ')
        self.execute_shell_on_device(device_target, 'input keyevent KEYCODE_ENTER')
        self.wait_with_log(constants.ONE_SEC)

    def push_vcf_contacts_to_device(self, device_target, path_to_contacts_file):
        """Pushing contacts file to device using adb command"""
        logging.info(
            'Pushing VCF contacts to device %s to destination <%s>',
            device_target.serial,
            constants.PHONE_CONTACTS_DESTINATION_PATH,
        )
        device_target.adb.push(
            [path_to_contacts_file, constants.PHONE_CONTACTS_DESTINATION_PATH],
            timeout=20,
        )

    def upload_vcf_contacts_to_device(self, device_target, path_to_contacts_file):
        """Upload contacts do device"""
        self.push_vcf_contacts_to_device(device_target, path_to_contacts_file)
        self.import_contacts_from_vcf_file(device_target)
        device_target.mbs.pressDevice()

    def verify_contact_name(self, expected_contact):
        actual_dialed_contact = self.device.mbs.getContactName()
        logging.info(
            'Expected contact name being called: <%s>, Actual: <%s>',
            expected_contact,
            actual_dialed_contact,
        )
        if actual_dialed_contact != expected_contact:
            raise CallUtilsError(
                "Actual and Expected contacts on dial pad don't match."
            )

    def wait_with_log(self, wait_time):
        """ Wait for specific time for debugging"""
        logging.info('Sleep for %s seconds', wait_time)
        time.sleep(wait_time)

    def open_bluetooth_media_app(self):
        """ Open Bluetooth Audio app """
        logging.info('Open Bluetooth Audio app')
        self.device.mbs.openMediaApp();
        self.wait_with_log(constants.WAIT_ONE_SEC)


    def open_bluetooth_palette(self):
        logging.info('Open Bluetooth Palette')
        self.device.mbs.openBluetoothPalette()

    def click_bluetooth_button(self):
        logging.info('Click Bluetooth Button')
        self.device.mbs.clickBluetoothButton()

    def is_bluetooth_connected(self):
        logging.info('Bluetooth Connected Status')
        is_connected = self.device.mbs.isBluetoothConnected()
        return is_connected

    def is_bluetooth_audio_disconnected_label_visible(self):
        """ Return is <Bluetooth Audio disconnected> label present """
        logging.info('Checking is <Bluetooth Audio disconnected> label present')
        actual_disconnected_label_status = self.device.mbs.isBluetoothAudioDisconnectedLabelVisible()
        logging.info('<Bluetooth Audio disconnected> label is present: %s',
                     actual_disconnected_label_status)
        return actual_disconnected_label_status
