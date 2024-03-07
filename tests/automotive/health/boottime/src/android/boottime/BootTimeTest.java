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

package android.boottime;

import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.device.LogcatReceiver;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.FileInputStreamSource;
import com.android.tradefed.result.InputStreamSource;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner.TestLogData;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner.TestMetrics;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.testtype.junit4.BeforeClassWithInfo;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.CommandStatus;
import com.android.tradefed.util.RunUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Performs successive reboots */
@RunWith(DeviceJUnit4ClassRunner.class)
@OptionClass(alias = "boot-time-test")
public class BootTimeTest extends BaseHostJUnit4Test {
    private static final String LOGCAT_CMD = "logcat *:V";
    private static final String LOGCAT_CMD_ALL = "logcat -b all *:V";
    private static final String LOGCAT_CMD_CLEAR = "logcat -c";
    private static final long LOGCAT_SIZE = 80 * 1024 * 1024;
    private static final String DMESG_FILE = "/data/local/tmp/dmesglogs.txt";
    private static final String DUMP_DMESG = String.format("dmesg > %s", DMESG_FILE);
    private static final String LOGCAT_FILENAME = "Successive_reboots_logcat";
    private static final String DMESG_FILENAME = "Successive_reboots_dmesg";
    private static final String IMMEDIATE_DMESG_FILENAME = "Successive_reboots_immediate_dmesg";
    private static final String F2FS_SHUTDOWN_COMMAND = "f2fs_io shutdown 4 /data";
    private static final String F2FS_SHUTDOWN_SUCCESS_OUTPUT = "Shutdown /data with level=4";

    @Option(
            name = "boot-count",
            description =
                    "Number of times to boot the devices to calculate the successive boot delay."
                            + " Second boot after the first boot will be skipped for correctness.")
    private int mBootCount = 5;

    @Option(
            name = "boot-delay",
            isTimeVal = true,
            description = "Time to wait between the successive boots.")
    private long mBootDelayTime = 2000;

    @Option(
            name = "after-boot-delay",
            isTimeVal = true,
            description = "Time to wait immediately after the successive boots.")
    private long mAfterBootDelayTime = 0;

    @Option(name = "device-boot-time", description = "Max time in ms to wait for device to boot.")
    protected long mDeviceBootTime = 5 * 60 * 1000;

    @Option(
            name = "successive-boot-prepare-cmd",
            description =
                    "A list of adb commands to run after first boot to prepare for successive"
                            + " boot tests")
    private List<String> mDeviceSetupCommands = new ArrayList<>();

    /**
     * Use this flag not to dump the dmesg logs immediately after the device is online. Use it only
     * if some of the boot dmesg logs are cleared when waiting until boot completed. By default this
     * is set to true which might result in duplicate logging.
     */
    @Option(
            name = "dump-dmesg-immediate",
            description =
                    "Whether to dump the dmesg logs" + "immediately after the device is online")
    private boolean mDumpDmesgImmediate = true;

    @Option(
            name = "force-f2fs-shutdown",
            description = "Force f2fs shutdown to trigger fsck check during the reboot.")
    private boolean mForceF2FsShutdown = false;

    @Option(
            name = "boot-time-pattern",
            description =
                    "Named boot time regex patterns which are used to capture signals in logcat and"
                            + " calculate duration between device boot to the signal being logged."
                            + " Key: name of custom boot metric, Value: regex to match single"
                            + " logcat line. Maybe repeated.")
    private Map<String, String> mBootTimePatterns = new HashMap<>();

    private LogcatReceiver mRebootLogcatReceiver = null;

    @Rule public TestLogData testLog = new TestLogData();
    @Rule public TestMetrics testMetrics = new TestMetrics();

    /**
     * Prepares the device for successive boots
     *
     * @param testInfo Test Information
     * @throws DeviceNotAvailableException if connection with device is lost and cannot be
     *     recovered.
     */
    @BeforeClassWithInfo
    public static void beforeClassWithDevice(TestInformation testInfo)
            throws DeviceNotAvailableException {
        ITestDevice testDevice = testInfo.getDevice();

        testDevice.enableAdbRoot();
        testDevice.setDate(null);
        testDevice.nonBlockingReboot();
        testDevice.waitForDeviceOnline();
        testDevice.waitForDeviceAvailable(0);
    }

    @Before
    public void setUp() throws Exception {
        ITestDevice testDevice = getDevice();
        setUpDeviceForSuccessiveBoots();
        String logcatCommand = mBootTimePatterns.isEmpty() ? LOGCAT_CMD_ALL : LOGCAT_CMD;
        mRebootLogcatReceiver = new LogcatReceiver(testDevice, logcatCommand, LOGCAT_SIZE, 0);
    }

    @Test
    public void testSuccessiveBoots() throws Exception {
        CLog.v("Waiting for %d msecs before successive boots.", mBootDelayTime);
        sleep(mBootDelayTime);
        for (int count = 0; count < mBootCount; count++) {
            testSuccessiveBoot(count);
        }
    }

    public void testSuccessiveBoot(int iteration) throws Exception {
        CLog.v("Successive boot iteration %d", iteration);
        getDevice().enableAdbRoot();
        // Property used for collecting the perfetto trace file on boot.
        getDevice().executeShellCommand("setprop persist.debug.perfetto.boottrace 1");
        if (mForceF2FsShutdown) {
            forseF2FsShutdown();
        }
        clearLogcat();
        sleep(5000);
        getDevice().nonBlockingReboot();
        getDevice().waitForDeviceOnline(mDeviceBootTime);
        getDevice().enableAdbRoot();
        if (mDumpDmesgImmediate) {
            saveDmesgInfo(String.format("%s_%d", IMMEDIATE_DMESG_FILENAME, iteration));
        }
        CLog.v("Waiting for %d msecs immediately after successive boot.", mAfterBootDelayTime);
        sleep(mAfterBootDelayTime);
        getDevice().waitForBootComplete(mDeviceBootTime);
        saveDmesgInfo(String.format("%s_%d", DMESG_FILENAME, iteration));
        saveLogcatInfo(iteration);
        // TODO(b/288323866): implement
        // analyzeBootLoaderTimingInfo(iteration);
    }

    private void setUpDeviceForSuccessiveBoots() throws DeviceNotAvailableException {
        for (String cmd : mDeviceSetupCommands) {
            CommandResult result;
            result = getDevice().executeShellV2Command(cmd);
            if (!CommandStatus.SUCCESS.equals(result.getStatus())) {
                CLog.w(
                        "Post boot setup cmd: '%s' failed, returned:\nstdout:%s\nstderr:%s",
                        cmd, result.getStdout(), result.getStderr());
            }
        }
    }

    private void forseF2FsShutdown() throws DeviceNotAvailableException {
        String output = getDevice().executeShellCommand(F2FS_SHUTDOWN_COMMAND).trim();
        if (!F2FS_SHUTDOWN_SUCCESS_OUTPUT.equalsIgnoreCase(output)) {
            CLog.e("Unable to shutdown the F2FS.");
        } else {
            CLog.i("F2FS shutdown successful.");
        }
    }

    private void saveLogcatInfo(int iteration) {
        try (InputStreamSource logcat = mRebootLogcatReceiver.getLogcatData()) {
            testLog.addTestLog(
                    String.format("%s_%d", LOGCAT_FILENAME, iteration), LogDataType.LOGCAT, logcat);
        }
    }

    private void saveDmesgInfo(String filename) throws DeviceNotAvailableException {
        getDevice().executeShellCommand(DUMP_DMESG);
        File dmesgFile = getDevice().pullFile(DMESG_FILE);
        testLog.addTestLog(
                filename, LogDataType.HOST_LOG, new FileInputStreamSource(dmesgFile, false));
    }

    private void clearLogcat() throws DeviceNotAvailableException {
        getDevice().executeShellCommand(LOGCAT_CMD_CLEAR);
        getDevice().clearLogcat();
        mRebootLogcatReceiver.clear();
    }

    private void sleep(long duration) {
        RunUtil.getDefault().sleep(duration);
    }
}
