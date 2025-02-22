#  Copyright (C) 2024 The Android Open Source Project
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

"""Android Nearby device setup."""

import base64
import dataclasses
import datetime
import time
from typing import Mapping

from mobly.controllers import android_device
from mobly.controllers.android_device_lib import adb

from betocq import gms_auto_updates_util
from betocq import nc_constants

WIFI_COUNTRYCODE_CONFIG_TIME_SEC = 3
TOGGLE_AIRPLANE_MODE_WAIT_TIME_SEC = 2
PH_FLAG_WRITE_WAIT_TIME_SEC = 3
WIFI_DISCONNECTION_DELAY_SEC = 3
ADB_RETRY_WAIT_TIME_SEC = 2

_DISABLE_ENABLE_GMS_UPDATE_WAIT_TIME_SEC = 2


read_ph_flag_failed = False

LOG_TAGS = [
    'Nearby',
    'NearbyMessages',
    'NearbyDiscovery',
    'NearbyConnections',
    'NearbyMediums',
    'NearbySetup',
]


def set_country_code(
    ad: android_device.AndroidDevice, country_code: str
) -> None:
  """Sets Wi-Fi and Telephony country code.

  When you set the phone to EU or JP, the available 5GHz channels shrinks.
  Some phones, like Pixel 2, can't use Wi-Fi Direct or Hotspot on 5GHz
  in these countries. Pixel 3+ can, but only on some channels.
  Not all of them. So, test Nearby Share or Nearby Connections without
  Wi-Fi LAN to catch any bugs and make sure we don't break it later.

  Args:
    ad: AndroidDevice, Mobly Android Device.
    country_code: WiFi and Telephony Country Code.
  """
  try:
    _do_set_country_code(ad, country_code)
  except adb.AdbError:
    ad.log.exception(
        f'Failed to set country code on device "{ad.serial}, try again.'
    )
    time.sleep(ADB_RETRY_WAIT_TIME_SEC)
    _do_set_country_code(ad, country_code)


def _do_set_country_code(
    ad: android_device.AndroidDevice, country_code: str
) -> None:
  """Sets Wi-Fi and Telephony country code."""
  if not ad.is_adb_root:
    ad.log.info(
        f'Skipped setting wifi country code on device "{ad.serial}" '
        'because we do not set country code on unrooted phone.'
    )
    return

  ad.log.info(f'Set Wi-Fi and Telephony country code to {country_code}.')
  ad.adb.shell('cmd wifi set-wifi-enabled disabled')
  time.sleep(WIFI_COUNTRYCODE_CONFIG_TIME_SEC)
  ad.adb.shell(
      'am broadcast -a com.android.internal.telephony.action.COUNTRY_OVERRIDE'
      f' --es country {country_code}'
  )
  ad.adb.shell(f'cmd wifi force-country-code enabled {country_code}')
  enable_airplane_mode(ad)
  time.sleep(WIFI_COUNTRYCODE_CONFIG_TIME_SEC)
  disable_airplane_mode(ad)
  ad.adb.shell('cmd wifi set-wifi-enabled enabled')
  telephony_country_code = (
      ad.adb.shell('dumpsys wifi | grep mTelephonyCountryCode')
      .decode('utf-8')
      .strip()
  )
  ad.log.info(f'Telephony country code: {telephony_country_code}')


def enable_logs(ad: android_device.AndroidDevice) -> None:
  """Enables Nearby related logs."""
  ad.log.info('Enable Nearby loggings.')
  for tag in LOG_TAGS:
    ad.adb.shell(f'setprop log.tag.{tag} VERBOSE')


def grant_manage_external_storage_permission(
    ad: android_device.AndroidDevice, package_name: str
) -> None:
  """Grants MANAGE_EXTERNAL_STORAGE permission to Nearby snippet."""
  try:
    _do_grant_manage_external_storage_permission(ad, package_name)
  except adb.AdbError:
    ad.log.exception(
        'Failed to grant MANAGE_EXTERNAL_STORAGE permission on device'
        f' "{ad.serial}", try again.'
    )
    time.sleep(ADB_RETRY_WAIT_TIME_SEC)
    _do_grant_manage_external_storage_permission(ad, package_name)


def _do_grant_manage_external_storage_permission(
    ad: android_device.AndroidDevice, package_name: str
) -> None:
  """Grants MANAGE_EXTERNAL_STORAGE permission to Nearby snippet."""
  build_version_sdk = int(ad.build_info['build_version_sdk'])
  if build_version_sdk < 30:
    return
  ad.log.info(
      f'Grant MANAGE_EXTERNAL_STORAGE permission on device "{ad.serial}".'
  )
  _grant_manage_external_storage_permission(ad, package_name)


def dump_gms_version(ad: android_device.AndroidDevice) -> Mapping[str, str]:
  """Dumps GMS version from dumpsys to sponge properties."""
  try:
    gms_version = _do_dump_gms_version(ad)
  except adb.AdbError:
    ad.log.exception(
        f'Failed to dump GMS version on device "{ad.serial}", try again.'
    )
    time.sleep(ADB_RETRY_WAIT_TIME_SEC)
    gms_version = _do_dump_gms_version(ad)
  return gms_version


def _do_dump_gms_version(ad: android_device.AndroidDevice) -> Mapping[str, str]:
  """Dumps GMS version from dumpsys to sponge properties."""
  out = (
      ad.adb.shell(
          'dumpsys package com.google.android.gms | grep "versionCode="'
      )
      .decode('utf-8')
      .strip()
  )
  return {f'GMS core version on {ad.serial}': out}


def toggle_airplane_mode(ad: android_device.AndroidDevice) -> None:
  """Toggles airplane mode on the given device."""
  ad.log.info('turn on airplane mode')
  enable_airplane_mode(ad)
  ad.log.info('turn off airplane mode')
  disable_airplane_mode(ad)


def connect_to_wifi_sta_till_success(
    ad: android_device.AndroidDevice, wifi_ssid: str, wifi_password: str
) -> datetime.timedelta:
  """Connecting to the specified wifi STA/AP."""
  ad.log.info('Start connecting to wifi STA/AP')
  wifi_connect_start = datetime.datetime.now()
  if not wifi_password:
    wifi_password = None
  connect_to_wifi(ad, wifi_ssid, wifi_password)
  return datetime.datetime.now() - wifi_connect_start


def connect_to_wifi(
    ad: android_device.AndroidDevice,
    ssid: str,
    password: str | None = None,
) -> None:
  if not ad.nearby.wifiIsEnabled():
    ad.nearby.wifiEnable()
  # return until the wifi is connected.
  password = password or None
  ad.log.info('Connect to wifi: ssid: %s, password: %s', ssid, password)
  ad.nearby.wifiConnectSimple(ssid, password)


def disconnect_from_wifi(ad: android_device.AndroidDevice) -> None:
  if not ad.is_adb_root:
    ad.log.info("Can't clear wifi network in non-rooted device")
    return
  ad.nearby.wifiClearConfiguredNetworks()
  time.sleep(WIFI_DISCONNECTION_DELAY_SEC)


def _grant_manage_external_storage_permission(
    ad: android_device.AndroidDevice, package_name: str
) -> None:
  """Grants MANAGE_EXTERNAL_STORAGE permission to Nearby snippet.

  This permission will not grant automatically by '-g' option of adb install,
  you can check the all permission granted by:
  am start -a android.settings.APPLICATION_DETAILS_SETTINGS
           -d package:{YOUR_PACKAGE}

  Reference for MANAGE_EXTERNAL_STORAGE:
  https://developer.android.com/training/data-storage/manage-all-files

  This permission will reset to default "Allow access to media only" after
  reboot if you never grant "Allow management of all files" through system UI.
  The appops command and MANAGE_EXTERNAL_STORAGE only available on API 30+.

  Args:
    ad: AndroidDevice, Mobly Android Device.
    package_name: The nearbu snippet package name.
  """
  try:
    ad.adb.shell(
        f'appops set --uid {package_name} MANAGE_EXTERNAL_STORAGE allow'
    )
  except adb.Error:
    ad.log.info('Failed to grant MANAGE_EXTERNAL_STORAGE permission.')


def enable_airplane_mode(ad: android_device.AndroidDevice) -> None:
  """Enables airplane mode on the given device."""
  try:
    _do_enable_airplane_mode(ad)
  except adb.AdbError:
    ad.log.exception(
        f'Failed to enable airplane mode on device "{ad.serial}", try again.'
    )
    time.sleep(ADB_RETRY_WAIT_TIME_SEC)
    _do_enable_airplane_mode(ad)


def _do_enable_airplane_mode(ad: android_device.AndroidDevice) -> None:
  if (ad.is_adb_root):
    ad.adb.shell(['settings', 'put', 'global', 'airplane_mode_on', '1'])
    ad.adb.shell([
        'am', 'broadcast', '-a', 'android.intent.action.AIRPLANE_MODE', '--ez',
        'state', 'true'
    ])
  ad.adb.shell(['svc', 'wifi', 'disable'])
  ad.adb.shell(['svc', 'bluetooth', 'disable'])
  time.sleep(TOGGLE_AIRPLANE_MODE_WAIT_TIME_SEC)


def disable_airplane_mode(ad: android_device.AndroidDevice) -> None:
  """Disables airplane mode on the given device."""
  try:
    _do_disable_airplane_mode(ad)
  except adb.AdbError:
    ad.log.exception(
        f'Failed to disable airplane mode on device "{ad.serial}", try again.'
    )
    time.sleep(ADB_RETRY_WAIT_TIME_SEC)
    _do_disable_airplane_mode(ad)


def _do_disable_airplane_mode(ad: android_device.AndroidDevice) -> None:
  if (ad.is_adb_root):
    ad.adb.shell(['settings', 'put', 'global', 'airplane_mode_on', '0'])
    ad.adb.shell([
        'am', 'broadcast', '-a', 'android.intent.action.AIRPLANE_MODE', '--ez',
        'state', 'false'
    ])
  ad.adb.shell(['svc', 'wifi', 'enable'])
  ad.adb.shell(['svc', 'bluetooth', 'enable'])
  time.sleep(TOGGLE_AIRPLANE_MODE_WAIT_TIME_SEC)


def check_if_ph_flag_committed(
    ad: android_device.AndroidDevice,
    pname: str,
    flag_name: str,
) -> bool:
  """Check if P/H flag is committed.

  Some devices don't support to check the flag with sqlite3. After the flag
  check fails for the first time, it won't try it again.

  Args:
    ad: AndroidDevice, Mobly Android Device.
    pname: The package name of the P/H flag.
    flag_name: The name of the P/H flag.

  Returns:
    True if the P/H flag is committed.
  """
  global read_ph_flag_failed
  if read_ph_flag_failed:
    return False
  sql_str = (
      'sqlite3 /data/data/com.google.android.gms/databases/phenotype.db'
      ' "select name, quote(coalesce(intVal, boolVal, floatVal, stringVal,'
      ' extensionVal)) from FlagOverrides where committed=1 AND'
      f' packageName=\'{pname}\';"'
  )
  try:
    flag_result = ad.adb.shell(sql_str).decode('utf-8').strip()
    return flag_name in flag_result
  except adb.AdbError:
    read_ph_flag_failed = True
    ad.log.exception('Failed to check PH flag')
  return False


def write_ph_flag(
    ad: android_device.AndroidDevice,
    pname: str,
    flag_name: str,
    flag_type: str,
    flag_value: str,
) -> None:
  """Write P/H flag."""
  ad.adb.shell(
      'am broadcast -a "com.google.android.gms.phenotype.FLAG_OVERRIDE" '
      f'--es package "{pname}" --es user "*" '
      f'--esa flags "{flag_name}" '
      f'--esa types "{flag_type}" --esa values "{flag_value}" '
      'com.google.android.gms'
  )
  time.sleep(PH_FLAG_WRITE_WAIT_TIME_SEC)


def check_and_try_to_write_ph_flag(
    ad: android_device.AndroidDevice,
    pname: str,
    flag_name: str,
    flag_type: str,
    flag_value: str,
) -> None:
  """Check and try to enable the given flag on the given device."""
  if(not ad.is_adb_root):
    ad.log.info(
        "Can't read or write P/H flag value in non-rooted device. Use Mobile"
        ' Utility app to config instead.'
    )
    return

  if check_if_ph_flag_committed(ad, pname, flag_name):
    ad.log.info(f'{flag_name} is already committed.')
    return
  ad.log.info(f'write {flag_name}.')
  write_ph_flag(ad, pname, flag_name, flag_type, flag_value)

  if check_if_ph_flag_committed(ad, pname, flag_name):
    ad.log.info(f'{flag_name} is configured successfully.')
  else:
    ad.log.info(f'failed to configure {flag_name}.')


def enable_bluetooth_multiplex(ad: android_device.AndroidDevice) -> None:
  """Enable bluetooth multiplex on the given device."""
  pname = 'com.google.android.gms.nearby'
  flag_name = 'mediums_supports_bluetooth_multiplex_socket'
  flag_type = 'boolean'
  flag_value = 'true'
  check_and_try_to_write_ph_flag(ad, pname, flag_name, flag_type, flag_value)


def enable_wifi_aware(ad: android_device.AndroidDevice) -> None:
  """Enable wifi aware on the given device."""
  pname = 'com.google.android.gms.nearby'
  flag_name = 'mediums_supports_wifi_aware'
  flag_type = 'boolean'
  flag_value = 'true'

  check_and_try_to_write_ph_flag(ad, pname, flag_name, flag_type, flag_value)


def enable_dfs_scc(ad: android_device.AndroidDevice) -> None:
  """Enable WFD/WIFI_HOTSPOT in a STA-associated DFS channel."""
  pname = 'com.google.android.gms.nearby'
  flag_name = 'mediums_lower_dfs_channel_priority'
  flag_type = 'boolean'
  flag_value = 'false'

  check_and_try_to_write_ph_flag(ad, pname, flag_name, flag_type, flag_value)


def disable_wlan_deny_list(ad: android_device.AndroidDevice) -> None:
  """Enable WFD/WIFI_HOTSPOT in a STA-associated DFS channel."""
  pname = 'com.google.android.gms.nearby'
  flag_name = 'wifi_lan_blacklist_verify_bssid_interval_hours'
  flag_type = 'long'
  flag_value = '0'

  check_and_try_to_write_ph_flag(ad, pname, flag_name, flag_type, flag_value)

  flag_name = 'mediums_wifi_lan_temporary_blacklist_verify_bssid_interval_hours'
  check_and_try_to_write_ph_flag(ad, pname, flag_name, flag_type, flag_value)


def enable_ble_scan_throttling_during_2g_transfer(
    ad: android_device.AndroidDevice, enable_ble_scan_throttling: bool = False
) -> None:
  """Enable BLE scan throttling during 2G transfer.
  """

  # The default values for the following parameters are 3 mins which are long
  # enough for the performance test.
  # mediums_ble_client_wifi_24_ghz_warming_up_duration
  # fast_pair_wifi_24_ghz_warming_up_duration
  # sharing_wifi_24_ghz_warming_up_duration

  pname = 'com.google.android.gms.nearby'
  flag_name = 'fast_pair_enable_connection_state_changed_listener'
  flag_type = 'boolean'
  flag_value = 'true' if enable_ble_scan_throttling else 'false'
  check_and_try_to_write_ph_flag(ad, pname, flag_name, flag_type, flag_value)

  flag_name = 'sharing_enable_connection_state_changed_listener'
  check_and_try_to_write_ph_flag(ad, pname, flag_name, flag_type, flag_value)

  flag_name = 'mediums_ble_client_enable_connection_state_changed_listener'
  check_and_try_to_write_ph_flag(ad, pname, flag_name, flag_type, flag_value)


def disable_redaction(ad: android_device.AndroidDevice) -> None:
  """Disable info log redaction on the given device."""
  pname = 'com.google.android.gms'
  flag_name = 'ClientLogging__enable_info_log_redaction'
  flag_type = 'boolean'
  flag_value = 'false'

  check_and_try_to_write_ph_flag(ad, pname, flag_name, flag_type, flag_value)


def install_apk(ad: android_device.AndroidDevice, apk_path: str) -> None:
  """Installs the apk on the given device."""
  ad.adb.install(['-r', '-g', '-t', apk_path])


def disable_gms_auto_updates(ad: android_device.AndroidDevice) -> None:
  """Disable GMS auto updates on the given device."""
  if not ad.is_adb_root:
    ad.log.warning(
        'You should disable the play store auto updates manually on a'
        'unrooted device, otherwise the test may be broken unexpected')
  ad.log.info('try to disable GMS Auto Updates.')
  gms_auto_updates_util.GmsAutoUpdatesUtil(ad).disable_gms_auto_updates()
  time.sleep(_DISABLE_ENABLE_GMS_UPDATE_WAIT_TIME_SEC)


def enable_gms_auto_updates(ad: android_device.AndroidDevice) -> None:
  """Enable GMS auto updates on the given device."""
  if not ad.is_adb_root:
    ad.log.warning(
        'You may enable the play store auto updates manually on a'
        'unrooted device after test.')
  ad.log.info('try to enable GMS Auto Updates.')
  gms_auto_updates_util.GmsAutoUpdatesUtil(ad).enable_gms_auto_updates()
  time.sleep(_DISABLE_ENABLE_GMS_UPDATE_WAIT_TIME_SEC)


def get_wifi_sta_frequency(ad: android_device.AndroidDevice) -> int:
  """Get wifi STA frequency on the given device."""
  wifi_sta_status = dump_wifi_sta_status(ad)
  if not wifi_sta_status:
    return nc_constants.INVALID_INT
  prefix = 'Frequency:'
  postfix = 'MHz'
  return get_int_between_prefix_postfix(wifi_sta_status, prefix, postfix)


def get_wifi_p2p_frequency(ad: android_device.AndroidDevice) -> int:
  """Get wifi p2p frequency on the given device."""
  wifi_p2p_status = dump_wifi_p2p_status(ad)
  if not wifi_p2p_status:
    return nc_constants.INVALID_INT
  prefix = 'channelFrequency='
  postfix = ', groupRole=GroupOwner'
  return get_int_between_prefix_postfix(wifi_p2p_status, prefix, postfix)


def get_wifi_sta_max_link_speed(ad: android_device.AndroidDevice) -> int:
  """Get wifi STA max supported Tx link speed on the given device."""
  wifi_sta_status = dump_wifi_sta_status(ad)
  if not wifi_sta_status:
    return nc_constants.INVALID_INT
  prefix = 'Max Supported Tx Link speed:'
  postfix = 'Mbps'
  return get_int_between_prefix_postfix(wifi_sta_status, prefix, postfix)


def get_int_between_prefix_postfix(
    string: str, prefix: str, postfix: str
) -> int:
  left_index = string.rfind(prefix)
  right_index = string.rfind(postfix)
  if left_index > 0 and right_index > left_index:
    try:
      return int(string[left_index + len(prefix): right_index].strip())
    except ValueError:
      return nc_constants.INVALID_INT
  return nc_constants.INVALID_INT


def dump_wifi_sta_status(ad: android_device.AndroidDevice) -> str:
  """Dumps wifi STA status on the given device."""
  try:
    return (
        ad.adb.shell('cmd wifi status | grep WifiInfo').decode('utf-8').strip()
    )
  except adb.AdbError:
    return ''


def dump_wifi_p2p_status(ad: android_device.AndroidDevice) -> str:
  """Dumps wifi p2p status on the given device."""
  try:
    return (
        ad.adb.shell('dumpsys wifip2p').decode('utf-8').strip()
    )
  except adb.AdbError:
    return ''


def get_hardware(ad: android_device.AndroidDevice) -> str:
  """Gets hardware information on the given device."""
  return ad.adb.getprop('ro.hardware')
