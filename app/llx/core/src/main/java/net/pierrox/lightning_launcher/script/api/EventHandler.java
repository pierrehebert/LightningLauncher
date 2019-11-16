package net.pierrox.lightning_launcher.script.api;

import net.pierrox.lightning_launcher.data.EventAction;

/**
 * Describe an event handler made of an action, some optional data and possibly a handler to execute after this one.
 * Handlers are chained in a linked list. The last handler in the list has no next.
 * When modifying an EventHandler acquired from {@link PropertySet#getEventHandler(String)}, you still need to call {@link PropertyEditor#setEventHandler(String, EventHandler)} to save changes.
 *
 * An instance of this object can be created with {@link #EventHandler(int, String)}; or retrieved with {@link PropertySet#getEventHandler(String)}.
 */
public class EventHandler {
    public static final int UNSET=0;
    public static final int NOTHING=1;
    public static final int APP_DRAWER=2;
    public static final int ZOOM_FULL_SCALE=3;
    public static final int ZOOM_TO_ORIGIN=4;
    public static final int SWITCH_FULL_SCALE_OR_ORIGIN=5;
    public static final int SHOW_HIDE_STATUS_BAR=6;
    public static final int LAUNCHER_MENU=7;
    public static final int EDIT_LAYOUT=8;
    public static final int CUSTOMIZE_LAUNCHER=9;
    /**
     * @deprecated use {@link #CUSTOMIZE_CONTAINER}
     */
    public static final int CUSTOMIZE_PAGE=10;
    public static final int CUSTOMIZE_CONTAINER=10;
    public static final int CUSTOMIZE_ITEM=11;
    public static final int ITEM_MENU=12;
    public static final int LAUNCH_ITEM=13;
    public static final int SEARCH=14;
    public static final int SHOW_HIDE_APP_MENU=15;
    public static final int SHOW_HIDE_APP_MENU_STATUS_BAR=16;
    public static final int SHOW_NOTIFICATIONS=17;
    /**
     * @deprecated use {@link #PREVIOUS_DESKTOP}
     */
    public static final int PREVIOUS_PAGE=18;
    public static final int PREVIOUS_DESKTOP=18;
    /**
     * @deprecated use {@link #NEXT_DESKTOP}
     */
    public static final int NEXT_PAGE=19;
    public static final int NEXT_DESKTOP=19;
    public static final int LAUNCH_APP=20;
    public static final int MOVE_ITEM=21;
    public static final int ADD_ITEM=22;
    public static final int LAUNCH_SHORTCUT=23;
    public static final int SELECT_WALLPAPER=24;
    public static final int GO_HOME=25;
    public static final int GO_HOME_ZOOM_TO_ORIGIN=26;
    public static final int SELECT_DESKTOP_TO_GO_TO=27;
    public static final int RESTART=28;
    public static final int CLOSE_TOPMOST_FOLDER=29;
    public static final int CLOSE_ALL_FOLDERS=30;
    public static final int SEARCH_APP=31;
    public static final int OPEN_FOLDER=32;
    public static final int GO_DESKTOP_POSITION=33;
    public static final int UNLOCK_SCREEN=34;
    public static final int RUN_SCRIPT=35;
    public static final int BACK=36;
    public static final int CUSTOM_MENU=37;
    public static final int USER_MENU=38;
    public static final int WALLPAPER_TAP=39;
    public static final int WALLPAPER_SECONDARY_TAP=40;
    public static final int SET_VARIABLE=41;
    public static final int SHOW_FLOATING_DESKTOP=42;
    public static final int HIDE_FLOATING_DESKTOP=43;
    public static final int OPEN_HIERARCHY_SCREEN=44;
    public static final int SHOW_APP_SHORTCUTS=45;
    public static final int CLOSE_APP_DRAWER=46;

    private EventAction mEventAction;

    /**
     * @hide
     */
    /*package*/ EventHandler(EventAction ea) {
        mEventAction = ea;
    }

    public EventHandler(int action, String data) {
        mEventAction = new EventAction(action, data);
    }

    public EventHandler(int action, String data, EventHandler next) {
        mEventAction = new EventAction(action, data, next==null ? null : next.getEventAction());
    }

    /**
     * Return the action to execute.
     * @return one of the possible actions defined by this class
     */
    public int getAction() {
        return mEventAction.action;
    }

    /**
     * Replaces the action for this handler.
     */
    public void setAction(int action) {
        mEventAction.action = action;
    }

    /**
     * An optional data associated to this handler.
     * Data can be set for the following actions:
     * <ul>
     *     <li>LAUNCH_APP/LAUNCH_SHORTCUT: Intent to start, as given by Intent.toUri</li>
     *     <li>RUN_SCRIPT: script id (converted to a string, or script id/data (if data are transmitted to the script)</li>
     *     <li>OPEN_FOLDER: container identifier (converted to a string). Note that any identifier can be used, but the result of passing a non folder container is undefined (although it may be useful...)</li>
     *     <li>GO_DESKTOP_POSITION: same content as the shortcut intent created through the Lightning Desktop position shortcut</li>
     * </ul>
     * @return can be null
     */
    public String getData() {
        return mEventAction.data;
    }

    /**
     * Set optional data for this action.
     * @param data @see {@link #getData()}
     */
    public void setData(String data) {
        mEventAction.data = data;
    }

    /**
     * Return the action to execute after this one.
     * @return the next event handler, or null if there is no other event handler
     */
    public EventHandler getNext() {
        if(mEventAction.next == null) {
            return null;
        } else {
            return new EventHandler(mEventAction.next);
        }
    }

    /**
     * Set the next element in the list.
     * Warning: the app doesn't check for loops in the linked list (for instance A.next=B and B.next=A),
     * but loops will make the app hang or crash, so be careful when chaining handlers.
     * @param next the next event handler, use null to terminate the list
     */
    public void setNext(EventHandler next) {
        mEventAction.next = next==null ? null : next.getEventAction();
    }

    /**
     * @hide
     */
    /*package*/ EventAction getEventAction() {
        return mEventAction;
    }
}
