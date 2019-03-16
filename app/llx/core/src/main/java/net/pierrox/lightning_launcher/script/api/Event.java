package net.pierrox.lightning_launcher.script.api;

import android.view.ViewGroup;

import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.script.api.screen.Screen;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;

/**
 * The event gather useful data dealing with the context of the event, such as which item caused the script to be executed, or what was the container at time of the event.
 * An instance of this object can be retrieved with {@link Lightning#getEvent_()}.
 */
public class Event {
	
	private Lightning mLightning;
	private Screen mScreen;
	private ItemView mItemView;
	private long mDate;
	private String mData;
    private String mSource;
	private ItemLayout mItemLayout;
	private float mLastTouchX;
	private float mLastTouchY;
	private float mLastTouchScreenX;
	private float mLastTouchScreenY;

	/**
	 * @hide
	 */
	public Event(Lightning lightning, Screen screen, String source, String data, long date, ItemLayout il, ItemView itemView) {
		mLightning = lightning;
		mScreen = screen;
		mItemView = itemView;
		mDate = date;
		mData = data;
        mSource = source;
		mItemLayout = il;
		mLastTouchX = screen.getLastTouchX();
		mLastTouchY = screen.getLastTouchY();
		mLastTouchScreenX = screen.getLastTouchScreenX();
		mLastTouchScreenY = screen.getLastTouchScreenY();
	}


	/**
	 * Access to the screen from which this event originates.
	 * Scripts run in the background are linked to an invisible screen too.
	 */
	public Screen getScreen() {
		return mScreen;
	}

	/**
	 * Access to the item from which this event originates. 
	 * Typically the item will be the tapped object when launching a script on tap event, but there will be no item when the event is a global one (such as desktop rotated, two finger swipe gestures, etc.). 
	 * @return an item or null if no item was involved in this event
	 */
	public Item getItem() {
		if(mItemView != null) {
			return mLightning.getCachedItem(mItemView);
		} else {
			return null;
		}
	}

	/**
	 * Returns the container in which the event has been triggered.
	 * This can be either a desktop, folder or panel.
	 * @return can be null if the event is not linked with a container
	 * @see #getContainerView()
	 */
	public Container getContainer() {
		ItemLayout il = mItemLayout == null ? mScreen.getScreen().getTargetOrTopmostItemLayout() : mItemLayout;
		return il == null ? null : mLightning.getCachedContainer(il);
	}
	
	/**
	 * Absolute X touch position at time of event.
	 * This data is available when touching the background and is not available when the event result of an item action or when the script is run in background.
	 * @return x position expressed in container coordinates, not in screen coordinates.
	 */
	public float getTouchX() {
		return mLastTouchX;
	}
	
	/**
	 * Absolute Y touch position at time of event.
	 * This data is available when touching the background and is not available when the event result of an item action or when the script is run in background.
	 * @return y position expressed in container coordinates, not in screen coordinates.
	 */
	public float getTouchY() {
		return mLastTouchY;
	}
	
	/**
	 * X touch position on the screen at time of event.
	 * This data is available when touching the background and is not available when the event result of an item action or when the script is run in background.
	 * @return x position expressed in screen coordinates.
	 */
	public float getTouchScreenX() {
		return mLastTouchScreenX;
	}
	
	/**
	 * Absolute Y touch position at time of event.
	 * This data is available when touching the background and is not available when the event result of an item action or when the script is run in background.
	 * @return y position expressed in screen coordinates.
	 */
	public float getTouchScreenY() {
		return mLastTouchScreenY;
	}

	/**
	 * Date at which the event occurred.
	 * @return number of milliseconds since January 1, 1970 00:00:00.0 UTC.
	 */
	public long getDate() {
		return mDate;
	}
	
	/**
	 * Optional data that may have passed to this script when run from {@link Screen#runScript(String, String)}.
	 */
	public String getData() {
		return mData;
	}

    /**
     * Return the event source as a string in the enumeration below. Names starting with "C_" refer to a container, whereas names starting with "I_" are linked with an item. For instance "C_CLICKED" is "container clicked" and "I_CLICKED" is "item clicked".
     * <ul>
     *     <li>K_HOME</li>
     *     <li>K_MENU</li>
     *     <li>K_MENU_L</li>
     *     <li>K_BACK</li>
     *     <li>K_BACK_L</li>
     *     <li>K_SEARCH</li>
     *     <li>I_CLICK</li>
     *     <li>I_LONG_CLICK</li>
     *     <li>I_SWIPE_LEFT</li>
     *     <li>I_SWIPE_RIGHT</li>
     *     <li>I_SWIPE_UP</li>
     *     <li>I_SWIPE_DOWN</li>
     *     <li>I_TOUCH</li>
     *     <li>I_PAUSED</li>
     *     <li>I_RESUMED</li>
     *     <li>C_LOADED</li>
     *     <li>C_RESUMED</li>
     *     <li>C_PAUSED</li>
     *     <li>C_CLICK</li>
     *     <li>C_LONG_CLICK</li>
     *     <li>C_DOUBLE_CLICK</li>
     *     <li>C_SWIPE_LEFT</li>
     *     <li>C_SWIPE_RIGHT</li>
     *     <li>C_SWIPE_UP</li>
     *     <li>C_SWIPE_DOWN</li>
     *     <li>C_SWIPE2_LEFT</li>
     *     <li>C_SWIPE2_RIGHT</li>
     *     <li>C_SWIPE2_UP</li>
     *     <li>C_SWIPE2_DOWN</li>
     *     <li>C_POSITION_CHANGED</li>
	 *     <li>C_ITEM_ADDED</li>
	 *     <li>C_ITEM_REMOVED</li>
     *     <li>MENU_APP</li>
     *     <li>MENU_ITEM</li>
     *     <li>MENU_CUSTOM</li>
     *     <li>STOP_POINT</li>
     *     <li>SHORTCUT</li>
     *     <li>PORTRAIT</li>
     *     <li>LANDSCAPE</li>
     *     <li>SCREEN_ON</li>
     *     <li>SCREEN_OFF</li>
     *     <li>RUN_SCRIPT</li>
     *     <li>APK (script loaded and executed from an APK)</li>
     *     <li>BACKGROUND (script started in background from another app)</li>
     *     <li>STARTUP</li>
     * </ul>
     */
    public String getSource() {
        return mSource;
    }
}
