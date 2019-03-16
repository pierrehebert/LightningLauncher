package net.pierrox.lightning_launcher.script.api;

import android.view.View;
import android.view.ViewGroup;

import org.mozilla.javascript.Function;

/**
 * Provides an access to the popup menu, so that it can be customized.
 *
 * An instance of this object can be retrieved by using directly the special variable 'menu' (which is the current Menu) when running a 'Menu' event.
 */
public class Menu {
    /** Main item menu in edit mode */
    public static final int MODE_ITEM_EM = 2;
    /** Main item menu in normal mode (not edit mode)*/
    public static final int MODE_ITEM_NO_EM = 12;
    /** The edit sub menu (Select an icon, Edit label, etc.) */
    public static final int MODE_ITEM_SUBMENU_EDIT = 3;
    /** The position sub menu (Detach from grid, Pin, etc.) */
    public static final int MODE_ITEM_SUBMENU_POSITION = 4;
    /** The action sub menu (App info, Play Store, etc.) */
    public static final int MODE_ITEM_SUBMENU_ACTION = 6;
    /** The scripts sub menu */
    public static final int MODE_ITEM_SUBMENU_SCRIPTS = 9;
    /** Custom item menu (populated with selected script entries)*/
    public static final int MODE_ITEM_CUSTOM = 10;
    /** The app shortcuts menu (Android 7.1+) */
    public static final int MODE_APP_SHORTCUTS = 15;

    /** Main container menu in edit mode */
    public static final int MODE_CONTAINER_EM = 1;
    public static final int MODE_CONTAINER_SUBMENU_SETTINGS = 7;
    public static final int MODE_CONTAINER_SUBMENU_ITEMS = 8;
    public static final int MODE_CONTAINER_SUBMENU_SCRIPTS = 9;
    /** Custom container menu (populated with selected script entries)*/
    public static final int MODE_CONTAINER_CUSTOM = 10;
    /** Main container menu in normal mode (not edit mode)*/
    public static final int MODE_CONTAINER_NO_EM = 13;
    public static final int MODE_CONTAINER_SUBMENU_SELECT = 14;

    /**
     * @hide
     */
    public interface MenuImpl {
        ViewGroup getRootView();
        ViewGroup getMainItemsView();
        int getMode();
        View addMainItem(String text, Function action);
        void close();
    }

    private MenuImpl mImpl;
    /**
     * @hide
     */
    public Menu(MenuImpl impl) {
        mImpl = impl;
    }

    /**
     * Return the root view for the menu. This view includes everything (title, icon buttons, main buttons)
     */
    public ViewGroup getRootView() {
        return mImpl.getRootView();
    }

    /** Return the items container, where the main textual buttons can be found */
    public ViewGroup getMainItemsView() {
        return mImpl.getMainItemsView();
    }

    /** Return the current menu mode. The mode is one of the constants above and gives a hint on the context for which the menu is displayed */
    public int getMode() {
        return mImpl.getMode();
    }

    /** Request this menu to be closed. This action is not automatically triggered when an item is clicked, it need to be done manually. */
    public void close() {
        mImpl.close();
    }
    /**
     * Add a main item, using the default style
     * @param text label to be displayed
     * @param action called when the item is clicked, this must be a function with a single "view" argument
     * @return the view created for the item
     */
    public View addMainItem(String text, Function action) {
        return mImpl.addMainItem(text, action);
    }
}
