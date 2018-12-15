package net.pierrox.lightning_launcher.data;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;

import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.TransformLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;

public class SavedItemGeometry {
    public Matrix transform;
    public Rect cell = new Rect(); // cell before to move handles, in grid mode
    public Rect bounds = new Rect(); // view bounds before to move handles, in free mode
    public int viewWidth;
    public int viewHeight;
    public int transformedViewWidth;
    public int transformedViewHeight;
    public int zOrder;

    public SavedItemGeometry(ItemView itemView) {
        Item item = itemView.getItem();
        transform = new Matrix(item.getTransform());
        cell.set(item.getCell());
        bounds.set(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom());
        if (itemView.isInitDone()) {
            View tv = itemView.getChildAt(0);
            transformedViewWidth = tv.getWidth();
            transformedViewHeight = tv.getHeight();
        } else {
            // init not done yet ? use the cell size
            transformedViewWidth = bounds.width();
            transformedViewHeight = bounds.height();
        }
        viewWidth = item.getViewWidth();
        viewHeight = item.getViewHeight();
        zOrder = item.getPage().items.indexOf(item);
    }

    public void applyTo(Item item) {
        item.getCell().set(cell);
        item.setTransform(transform, false);
        item.setViewWidth(viewWidth);
        item.setViewHeight(viewHeight);
    }
}
