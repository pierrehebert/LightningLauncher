package net.pierrox.lightning_launcher.script.api;

import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.script.api.screen.HomeScreen;

/**
 * Access to some of the general launcher configuration data.
 * An instance of this object can be retrieved with {@link Lightning#getConfiguration()}.
 */
public class Configuration {
    private Lightning mLightning;
    private GlobalConfig mGlobalConfig;

    /**
     * @hide
     */
    /*package*/ Configuration(Lightning lightning) {
        mLightning = lightning;
        mGlobalConfig = mLightning.getEngine().getGlobalConfig();
    }

    /**
     * Returns the home desktop, the one used for the "Go to home desktop" action.
     */
    public int getHomeDesktopId() {
        return mGlobalConfig.homeScreen;
    }

    /**
     * Set which desktop to use as the home desktop.
     * Please note that this will not automatically change the currently displayed desktop. Use {@link net.pierrox.lightning_launcher.script.api.screen.HomeScreen#goToDesktopPosition(int, float, float, float, boolean)} for this.
     * @param desktopId identifier of the desktop to use as the home desktop
     */
    public void setHomeDesktopId(int desktopId) {
        if(Page.isDashboard(desktopId)) {
            mGlobalConfig.homeScreen = desktopId;
            mLightning.getEngine().notifyGlobalConfigChanged();
        }
    }

    /**
     * Retrieve the current desktop id. The current desktop is the one to display in the home
     * screen when starting the app. This is the same as {@link HomeScreen#getCurrentDesktop().id}
     * but is accessible even when the home screen has not been created. The current desktop is
     * saved to persistent storage so that the launcher can display it when relaunched. This can be
     * seen as "the last viewed desktop".
     * This function is meant to be used by background script when the home screen is not available.
     */
    public int getCurrentDesktopId() {
        return mLightning.getEngine().readCurrentPage(Page.FIRST_DASHBOARD_PAGE);
    }

    /**
     * Set which desktop to display when the launcher is started. This will not change the currently
     * displayed desktop, if the home screen is visible. Note that this value is also set when calling.
     * {@link HomeScreen#goToDesktop(int)}. This function is meant to be used by background script when
     * the home screen is not available.
     * @param desktopId
     */
    public void setCurrentDesktopId(int desktopId) {
        if(Page.isDashboard(desktopId)) {
            mLightning.getEngine().writeCurrentPage(desktopId);
        }
    }

    /**
     * Returns the desktop used as the lock screen, or undefined if not set.
     * @return identifier, or {@link Container#NONE} if not set
     */
    public int getLockscreenDesktopId() {
        return mGlobalConfig.lockScreen;
    }

    /**
     * Set which desktop to use on the lock screen.
     * @param desktopId identifier of the desktop to use as the lock screen. Use {@link Container#NONE} to disable it.
     */
    public void setLockscreenDesktopId(int desktopId) {
        mGlobalConfig.lockScreen = desktopId;
        mLightning.getEngine().notifyGlobalConfigChanged();
    }

    /**
     * Returns the desktop used as the floating desktop, or undefined if not set.
     * @return identifier, or {@link Container#NONE} if not set
     */
    public int getFloatingDesktopId() {
        return mGlobalConfig.overlayScreen;
    }

    /**
     * Set which desktop to use as the floating desktop.
     * @param desktopId identifier of the desktop to use as the floating one. Using {@link Container#NONE} to display a blank screen.
     */
    public void setFloatingDesktopId(int desktopId) {
        mGlobalConfig.overlayScreen = desktopId;
        mLightning.getEngine().notifyGlobalConfigChanged();
    }

    /**
     * Returns the desktop used as the live wallpaper, or undefined if not set.
     * @return identifier, or {@link Container#NONE} if not set
     */
    public int getLiveWallpaperDesktopId() {
        return mGlobalConfig.lwpScreen;
    }

    /**
     * Set which desktop to use as the live wallpaper. This isn't enough to activate the live wallpaper, Lightning must also be manually selected as the current live wallpaper engine.
     * @param desktopId identifier of the desktop to use as the live wallpaper one. Using {@link Container#NONE} will not disable the live wallpaper but display a blank screen.
     */
    public void setLiveWallpaperDesktopId(int desktopId) {
        mGlobalConfig.lwpScreen = desktopId;
        mLightning.getEngine().notifyGlobalConfigChanged();
    }

    /**
     * Returns an array of desktop identifiers, in the order specified in the configuration.
     */
    public int[] getAllDesktops() {
        return mGlobalConfig.screensOrder;
    }

    public PropertySet getProperties() {
        return new PropertySet(mLightning, mGlobalConfig);
    }
}
