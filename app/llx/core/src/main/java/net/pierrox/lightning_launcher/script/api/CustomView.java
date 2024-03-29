package net.pierrox.lightning_launcher.script.api;

import android.view.View;

import net.pierrox.lightning_launcher.views.item.CustomViewView;
import net.pierrox.lightning_launcher.views.item.ItemView;

/**
 * A CustomView is an advance special item that can display any Android View.
 *
 * An instance of this object can be retrieved with {@link Container#addCustomView(float, float)}; or with any function that returns an {@link Item} when that returned item is a CustomView; or by using directly the special variable 'item' (which is the current CustomView) when running a 'Create/Destroy script' event.
 */
public class CustomView extends Item {
    /**
     * @hide
     */
    public CustomView(Lightning lightning, ItemView itemView) {
        super(lightning, itemView);
    }

    /**
     * Provides the view which has been built by the create script
     * @return a view, can be null
     */
    public View getView() {
        return ((CustomViewView)mItemView).getView();
    }

    /**
     * Specify whether this view grabs events in the horizontal direction
     * Grabbing will prevent the container to scroll. This behavior is needed when the custom view contains scrollable components (a seek bar, a scroll view)
     * @param grab whether to grab events in the horizontal direction
     */
    public void setHorizontalGrab(boolean grab) {
        ((CustomViewView)mItemView).setHorizontalGrab(grab);
    }

    /**
     * Return the current horizontal grab setting. Default is off.
     */
    public boolean getHorizontalGrab() {
        return ((CustomViewView)mItemView).hasHorizontalGrab();
    }

    /**
     * Specify whether this view grabs events in the vertical direction.
     * Grabbing will prevent the container to scroll. This behavior is needed when the custom view contains scrollable components (a list view, a scroll view, etc.)
     * @param grab whether to grab events in the vertical direction
     */
    public void setVerticalGrab(boolean grab) {
        ((CustomViewView)mItemView).setVerticalGrab(grab);
    }

    /**
     * Return the current vertical grab setting. Default is off.
     */
    public boolean getVerticalGrab() {
        return ((CustomViewView)mItemView).hasVerticalGrab();
    }
}
