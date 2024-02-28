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
package android.platform.test.rule

import android.app.role.RoleManager
import android.os.Build
import android.platform.helpers.notesrole.NotesRoleUtil
import org.junit.runner.Description

/**
 * This rule allows end-to-end tests to manage requirements pertaining to [RoleManager.ROLE_NOTES].
 *
 * @param requiredAndroidVersion enforce required Android version, default is Android U
 * @param requiredNotesRoleHolderPackage the package name for the [RoleManager.ROLE_NOTES] holder
 *   that should be used in the tests. The rule will take care of setting and restoring the role
 *   holder for each test. Test will be skipped if the provided package is not installed. The rule
 *   will also ensure to kill this app before and after the test to ensure clean state.
 */
class NotesRoleManagerRule
@JvmOverloads
constructor(
    private val requiredAndroidVersion: Int = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    private val requiredNotesRoleHolderPackage: String,
) : TestWatcher() {

    private val notesRoleUtil = NotesRoleUtil(context)
    private var prevNotesRoleHolder: String? = null

    override fun starting(description: Description?) {
        super.starting(description)

        // Check if the device configuration can run the test. Assumption is used instead of
        // assertion because not all devices have the Notes role or the required app installed.
        // So this rule takes care of skipping the test on incompatible devices. This is a
        // workaround to run the test on select few devices.
        notesRoleUtil.checkAndroidRolePackageAssumptions(
            requiredAndroidVersion,
            requiredNotesRoleHolderPackage
        )

        prevNotesRoleHolder = notesRoleUtil.getRoleHolderPackageName()
        notesRoleUtil.setRoleHolder(requiredNotesRoleHolderPackage)

        // Kill the supplied Notes role holder app to avoid issues during verification in test.
        notesRoleUtil.forceStopPackage(requiredNotesRoleHolderPackage)
    }

    override fun finished(description: Description?) {
        super.finished(description)

        notesRoleUtil.forceStopPackage(requiredNotesRoleHolderPackage)

        prevNotesRoleHolder?.let { notesRoleUtil.setRoleHolder(it) }
    }
}
