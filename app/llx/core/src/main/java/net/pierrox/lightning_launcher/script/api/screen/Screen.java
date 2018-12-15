package net.pierrox.lightning_launcher.script.api.screen;

import android.content.Context;
import android.content.Intent;

import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.api.Container;
import net.pierrox.lightning_launcher.script.api.Desktop;
import net.pierrox.lightning_launcher.script.api.Event;
import net.pierrox.lightning_launcher.script.api.Folder;
import net.pierrox.lightning_launcher.script.api.Item;
import net.pierrox.lightning_launcher.script.api.Lightning;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;

import java.util.ArrayList;

/**
 * The screen is the surface where user interactions occur. Example of known screens are:
 * <ul>
 *     <li>the home screen (see {@link HomeScreen})</li>
 *     <li>the app drawer (see {@link ActivityScreen})</li>
 *     <li>the lock screen (see {@link ActivityScreen})</li>
 *     <li>the floating desktop</li>
 * </ul>
 */
public class Screen {
    private static final String EVENT_SOURCE_SCRIPT = "RUN_SCRIPT";

    protected Lightning mLightning;
    protected net.pierrox.lightning_launcher.engine.Screen mScreen;

    /**
     * @hide
     */
    public Screen(Lightning lightning, net.pierrox.lightning_launcher.engine.Screen screen) {
        mLightning = lightning;
        mScreen = screen;
    }

    /**
     * @hide
     */
    public net.pierrox.lightning_launcher.engine.Screen getScreen() {
        return mScreen;
    }

    @Override
    public String toString() {
        return mScreen.getIdentity().toString();
    }

    /**
     * Returns the currently displayed desktop.
     */
    public Desktop getCurrentDesktop() {
        return (Desktop) mLightning.getCachedContainer(mScreen.getCurrentRootItemLayout());
    }

    /**
     * Returns the first container found with this id.
     * As of Lightning V14, a single container may be displayed at several locations. This function returns the first one found, in no particular order.
     * Calling this function can possibly be expensive because the container and its parents may have to be loaded on the fly.
     * @see #getAllContainersById(int)
     */
    public Container getContainerById(int id) {
        ItemLayout[] ils = mScreen.getItemLayoutsForPage(id);
        if(ils.length > 0) {
            return mLightning.getCachedContainer(ils[0]);
        } else {
            ItemLayout il = mScreen.prepareFirstItemLayout(id);
            return il==null ? null : mLightning.getCachedContainer(il);
        }
    }

    /**
     * Returns an item by its id. This is a shortcut avoiding to traverse the list of all desktops and folders.
     * @param id item identifier
     * @return an item, or null if this id is not known.
     * @see #getAllItemsById(int)
     */
    public Item getItemById(int id) {
        int page = Utils.getPageForItemId(id);
        ItemLayout itemLayout = mScreen.prepareFirstItemLayout(page);
        if(itemLayout == null) {
            return null;
        }
        ItemView itemView = itemLayout.getItemView(id);
        if(itemView == null) {
            return null;
        }
        return mLightning.getCachedItem(itemView);
    }

    /**
     * Returns all containers for a given id.
     * Warning: this is an expensive call.
     */
    public Container[] getAllContainersById(int id) {
        ArrayList<ItemLayout> itemLayouts = mScreen.prepareAllItemLayouts(id);
        int n = itemLayouts.size();
        Container[] containers = new Container[n];
        for(int i=0; i<n; i++) {
            containers[i] = mLightning.getCachedContainer(itemLayouts.get(i));
        }

        return containers;
    }

    /**
     * Returns all items for a given id.
     * Warning: this is an expensive call.
     */
    public Item[] getAllItemsById(int id) {
        ArrayList<ItemLayout> itemLayouts = mScreen.prepareAllItemLayouts(Utils.getPageForItemId(id));
        int n = itemLayouts.size();
        Item[] items = new Item[n];
        for(int i=0; i<n; i++) {
            ItemView itemView = itemLayouts.get(i).getItemView(id);
            if(itemView == null) {
                // bad item id, stop here and returns an empty array
                return new Item[0];
            }
            items[i] = mLightning.getCachedItem(itemView);
        }

        return items;
    }

    /**
     * Returns the focused container.
     * The focused container is usually the last touched container, or the last open folder. This can be either a desktop, folder or panel.
     * @return can be null if no container has been focused yet.
     */
    public Container getFocusedContainer() {
        ItemLayout il = mScreen.getTargetItemLayout();
        if(il == null) {
            return null;
        } else {
            return mLightning.getCachedContainer(il);
        }
    }

    /**
     * Absolute X position in the container space of the last touch event.
     * This function returns Integer.MIN_VALUE if no touch has been registered yet.
     * @return x position expressed in container coordinates, not in screen coordinates.
     */
    public float getLastTouchX() {
        return mScreen.getLastTouchedAddX();
    }

    /**
     * Absolute Y position in the container space of the last touch event.
     * This function returns Integer.MIN_VALUE if no touch has been registered yet.
     * @return y position expressed in container coordinates, not in screen coordinates.
     */
    public float getLastTouchY() {
        return mScreen.getLastTouchedAddY();
    }

    /**
     * Absolute X position in the screen space of the last touch event.
     * This function returns Integer.MIN_VALUE if no touch has been registered yet.
     * @return x position expressed in screen coordinates.
     */
    public float getLastTouchScreenX() {
        ItemLayout il = mScreen.getTargetItemLayout();
        if(il != null) {
            float[] pos = mScreen.translateItemLayoutCoordsIntoScreenCoords(il, mScreen.getLastTouchedAddX(), 0);
            return pos[0];
        } else {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Absolute Y position in the screen space of the last touch event.
     * This function returns Integer.MIN_VALUE if no touch has been registered yet.
     * @return y position expressed in screen coordinates.
     */
    public float getLastTouchScreenY() {
        ItemLayout il = mScreen.getTargetItemLayout();
        if(il != null) {
            float[] pos = mScreen.translateItemLayoutCoordsIntoScreenCoords(il, 0, mScreen.getLastTouchedAddY());
            return pos[0];
        } else {
            return Integer.MIN_VALUE;
        }
    }


    /**
     * Same as #runAction(int,String) with a null data.
     */
    public void runAction(int action) {
        mScreen.runAction(mLightning.getEngine(), EVENT_SOURCE_SCRIPT, new EventAction(action, null));
    }

    /**
     * Same as #runAction(int,Item,String) with a null item and data.
     */
    public void runAction(int action, String data) {
        mScreen.runAction(mLightning.getEngine(), EVENT_SOURCE_SCRIPT, new EventAction(action, data));
    }

    /**
     * Run a Lightning action. This method does nothing when the script is run in background.
     * @param action action code (one of the values defined in {@link net.pierrox.lightning_launcher.script.api.EventHandler}
     * @param item item to be used as the target (only useful with actions requiring an item)
     * @param data optional data to send to be used by the action, use null if none
     */
    public void runAction(int action, Item item, String data) {
        mScreen.runAction(mLightning.getEngine(), EVENT_SOURCE_SCRIPT, new EventAction(action, data), item.getItemView());
    }

    /**
     * Run another script. Same as calling runScript(null, name, data) with a null path.
     * @see #runScript(String, String, String)
     */
    public void runScript(final String name, final String data) {
        runScript(null, name, data);
    }

    /**
     * Run another script.
     * Optional data can be transmitted to the called script and retrieved using {@link Event#getData()}.
     * @param path path of the script (null to look in all directories)
     * @param name name of the script as found in the script editor, optionally prefixed with its path
     * @param data optional data to send to the script. Use JSON to pass more than a string.
     */
    public void runScript(final String path, final String name, final String data) {
        final ScriptExecutor scriptExecutor = mLightning.getEngine().getScriptExecutor();
        scriptExecutor.getHandler().post(new Runnable() {
            @Override
            public void run() {
                scriptExecutor.runScript(mScreen, path, name, "RUN_SCRIPT", data);
            }
        });
    }

    /**
     * Returns whether the current screen is paused. It often means it is not displayed.
     */
    public boolean isPaused() {
        return mScreen.isPaused();
    }


    /**
     * Returns the list of currently open folders. This function returns the opener item, not the container itself.
     * This method will return null when the script is executed in the background.
     * @return an Array of Folder items, sorted top to bottom (topmost folder is at index 0).
     */
    public Folder[] getOpenFolders() {
        ItemView[] folders = mScreen.getOpenFolders();
        if(folders != null) {
            int n = folders.length;
            Folder[] script_folders = new Folder[n];
            for (int i = 0; i < n; i++) {
                script_folders[i] = (Folder) mLightning.getCachedItem(folders[i]);
            }
            return script_folders;
        } else {
            return null;
        }
    }

    /**
     * Start an activity.
     * Example:<code><pre>
     * var intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.pierrox.net/")
     * LL.startActivity(intent);</pre></code>
     * @param intent intent to start the activity
     * @return true if launch is successful, false if activity not found or permission denied
     */
    public boolean startActivity(Intent intent) {
        try {
            getContext().startActivity(intent);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    /**
     * Return the Android Context this screen is linked with (an activity context for the home, app drawer and lock screens, a service context for the floating desktop).
     * This is meant to be used with Android services.
     */
    public Context getContext() {
        return mScreen.getContext();
    }
}
