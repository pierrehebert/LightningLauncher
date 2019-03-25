package net.pierrox.lightning_launcher.script.api;

import net.pierrox.lightning_launcher.views.item.ItemView;

/**
 * Represents a Page Indicator Item. No extra methods available.
 *
 * An instance of this object can be retrieved with any function that returns an {@link Item} when that returned item is a PageIndicator; or with {@link Container#addPageIndicator(float, float)}.
 */
public class PageIndicator extends Item {
    /**
     * @hide
     */
    public PageIndicator(Lightning lightning, ItemView itemView) {
        super(lightning, itemView);
    }
}
