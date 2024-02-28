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

package android.platform.helpers;

import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiObject2;

/** {@inheritDoc} */
public interface IChromeHelper2 extends IAppHelper2 {
    enum MenuItem {
        BOOKMARKS("Bookmarks"),
        NEW_TAB("New tab"),
        CLOSE_ALL_TABS("Close all tabs"),
        DOWNLOADS("Downloads"),
        HISTORY("History"),
        SETTINGS("Settings");

        private final String mDisplayName;

        MenuItem(String displayName) {
            mDisplayName = displayName;
        }

        @Override
        public String toString() {
            return mDisplayName;
        }
    }

    enum ClearRange {
        PAST_HOUR("past hour"),
        PAST_DAY("past day"),
        PAST_WEEK("past week"),
        LAST_4_WEEKS("last 4 weeks"),
        BEGINNING_OF_TIME("beginning of time");

        private final String mDisplayName;

        ClearRange(String displayName) {
            mDisplayName = displayName;
        }

        @Override
        public String toString() {
            return mDisplayName;
        }
    }

    /**
     * Setup expectations: Chrome is open and on a standard page, i.e. a tab is open.
     *
     * <p>This method will open the URL supplied and block until the page is open.
     */
    void openUrl(String url);

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method will scroll the page as directed and block until idle.
     */
    void flingPage(Direction dir);

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method will open the overload menu, indicated by three dots and block until open.
     */
    void openMenu();

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method will open provided item in the menu.
     */
    void openMenuItem(IChromeHelper2.MenuItem menuItem);

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method will open provided item in the menu.
     *
     * @param menuItem The name of menu item.
     * @param waitForPageLoad Wait for the page to load completely or not.
     */
    default void openMenuItem(IChromeHelper2.MenuItem menuItem, boolean waitForPageLoad) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method will add a new tab and land on the webpage of given url.
     */
    void addNewTab(String url);

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method will go to tab switcher by clicking tab switcher button.
     */
    void openTabSwitcher();

    /**
     * Setup expectations: Chrome is open on a page or in tab switcher.
     *
     * <p>This method will switch to the tab at tabIndex.
     */
    void switchTab(int tabIndex);

    /**
     * Setup expectations: Chrome has at least one tab.
     *
     * <p>This method will close all tabs.
     */
    void closeAllTabs();

    /**
     * Setup expectations: Chrome is open on a page and the tabs are treated as apps.
     *
     * <p>This method will change the settings to treat tabs inside of Chrome and block until Chrome
     * is open on the original tab.
     */
    void mergeTabs();

    /**
     * Setup expectations: Chrome is open on a page and the tabs are merged.
     *
     * <p>This method will change the settings to treat tabs outside of Chrome and block until
     * Chrome is open on the original tab.
     */
    void unmergeTabs();

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method will reload the page by clicking the refresh button, and block until the page
     * is reopened.
     */
    void reloadPage();

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method will stop loading page then reload the page by clicking the refresh button,
     * and block until the page is reopened.
     */
    default void stopAndReloadPage() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method will stop loading page then reload the page by clicking the refresh button,
     *
     * @param waitForPageLoad Wait for the page to load completely or not.
     */
    default void stopAndReloadPage(boolean waitForPageLoad) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method is getter for contentDescription of Tab elements.
     */
    String getTabDescription();

    /**
     * Setup expectations: Chrome is open on a History page.
     *
     * <p>This method clears browser history for provided period of time.
     */
    void clearBrowsingData(IChromeHelper2.ClearRange range);

    /**
     * Setup expectations: Chrome is open on a Downloads page.
     *
     * <p>This method checks header is displayed on Downloads page.
     */
    void checkIfDownloadsOpened();

    /**
     * Setup expectations: Chrome is open on a Settings page.
     *
     * <p>This method clicks on Privacy setting on Settings page.
     */
    void openPrivacySettings();

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>This method will add the current page to Bookmarks
     */
    default boolean addCurrentPageToBookmark() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Chrome is open on a Bookmarks page.
     *
     * <p>This method selects a bookmark from the Bookmarks page.
     *
     * @param index The Index of bookmark to select.
     */
    default void openBookmark(int index) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Chrome is open on a Bookmarks page.
     *
     * <p>This method selects a bookmark from the Bookmarks page.
     *
     * @param bookmarkName The string of the target bookmark to select.
     * @param waitForPageLoad Wait for the page to load completely or not.
     */
    default void openBookmark(String bookmarkName, boolean waitForPageLoad) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>Selects the link with specific text.
     *
     * @param linkText The text of the link to select.
     */
    default void selectLink(String linkText) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>Performs a scroll gesture on the page.
     *
     * @param dir The direction on the page to scroll.
     * @param percent The distance to scroll as a percentage of the page's visible size.
     */
    default void scrollPage(Direction dir, float percent) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectations: Chrome is open on a page.
     *
     * <p>Get the UiObject2 of the page screen.
     */
    default UiObject2 getWebPage() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectation: Chrome was loading a web page.
     *
     * <p>Returns a boolean to state if current page is loaded.
     */
    default boolean isWebPageLoaded() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Setup expectation: Chrome was loading a web page.
     *
     * <p>Checks number of active tabs.
     */
    default void tabsCount(int number) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }
}
