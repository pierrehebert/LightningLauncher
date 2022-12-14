/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher.views;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.StopPoint;
import net.pierrox.lightning_launcher.views.item.ItemView;

import java.util.ArrayList;

/**
* Created by pierrot on 18/03/2015.
*/
public class SnappingContext {
    public static final int SNAP_LEFT = 1;
    public static final int SNAP_RIGHT = 2;
    public static final int SNAP_TOP = 4;
    public static final int SNAP_BOTTOM = 8;
    public static final int SNAP_CENTER = 16;
    public static final int SNAP_ALL = 31;

    private int snap_what;
    private float touch_slop;
    public float min_dx = Float.MAX_VALUE;
    public float min_dy = Float.MAX_VALUE;
    public float anchor_x = Float.MAX_VALUE;
    public float anchor_y = Float.MAX_VALUE;
    private ArrayList<ItemView> snapped_items;
    private ArrayList<RectF> snapped_bounds;
    private ItemView my_item_view;
    public ItemLayout item_layout;

    public SnappingContext(ItemView my_item_view, float touch_slop, int snap_what) {
        this.my_item_view = my_item_view;
        this.item_layout = my_item_view.getParentItemLayout();
        this.touch_slop = touch_slop;
        snapped_items = new ArrayList<>(3);
        snapped_bounds = new ArrayList<>(3);
        this.snap_what = snap_what;
    }

    public void stop() {
        for(ItemView i : snapped_items) {
            i.setFocused(false);
        }
        my_item_view.setFocused(false);
        snapped_items = null;
        snapped_bounds = null;
        item_layout.setSnappingData(Float.MAX_VALUE, Float.MAX_VALUE, null);
    }

    public void computeSnaps(Matrix item_transform) {
        Rect hr = new Rect();
        item_layout.getHitRect(hr);
        RectF screen_bounds=new RectF(hr);

        // compute corners and center of the moving item
        RectF bounds_from = new RectF();
        RectF bounds_to = new RectF();

        computeItemViewSnapBounds(my_item_view, item_transform, bounds_from);

        for(ItemView i : snapped_items) {
            i.setFocused(false);
        }
        my_item_view.setFocused(false);

        min_dx = Float.MAX_VALUE;
        min_dy = Float.MAX_VALUE;
        anchor_x = Float.MAX_VALUE;
        anchor_y = Float.MAX_VALUE;
        snapped_items.clear();
        snapped_bounds.clear();

        // first pass to compute the minimum distance
        for(int n=item_layout.getChildCount()-1; n>=0; n--) {
            View v = item_layout.getChildAt(n);
            if(v instanceof ItemView) {
                ItemView i = (ItemView) v;
                if (i.isSelected() || i == my_item_view) continue;

                // compute position of the item
                computeItemViewSnapBounds(i, i.getItem().getTransform(), bounds_to);

                // exclude items not visible on the screen
                if (RectF.intersects(screen_bounds, bounds_to)) {
                    checkSnap(bounds_from, bounds_to);
                }
            }
        }

        // second pass to find item snapping with the minimum distance
        for(int n=item_layout.getChildCount()-1; n>=0; n--) {
            View v = item_layout.getChildAt(n);
            if(v instanceof ItemView) {
                ItemView i = (ItemView) v;
                if (i.isSelected() || i == my_item_view) continue;

                // compute position of the item
                computeItemViewSnapBounds(i, i.getItem().getTransform(), bounds_to);

                // exclude items not visible on the screen
                if (RectF.intersects(screen_bounds, bounds_to)) {
                    if (checkSnap(bounds_from, bounds_to)) {
                        snapped_items.add(i);
                        snapped_bounds.add(new RectF(bounds_to));
                    }
                }
            }
        }

        for(ItemView i : snapped_items) {
            i.setFocused(true);
        }
    }

    public void applySnaps(Matrix item_transform) {
        item_layout.setSnappingData(anchor_x, anchor_y, snapped_bounds);

        if(snapped_bounds.size() > 0) {
            RectF bounds_to = new RectF();
            computeItemViewSnapBounds(my_item_view, item_transform, bounds_to);
            snapped_bounds.add(bounds_to);
            my_item_view.setFocused(true);
        }
    }

    private void computeItemViewSnapBounds(ItemView itemView, Matrix itemTransform, RectF outBounds) {
        Item item = itemView.getItem();
        float cw = item_layout.getCellWidth();
        float ch = item_layout.getCellHeight();
        if(item.getItemConfig().onGrid) {
            outBounds.set(item.getCell());
            outBounds.left *= cw;
            outBounds.right *= cw;
            outBounds.top *= ch;
            outBounds.bottom *= ch;
        } else {
            outBounds.set(0, 0, item.getViewWidth(), item.getViewHeight());
            itemTransform.mapRect(outBounds);
        }
        Matrix t=item_layout.getTransformForItemView(itemView);
        if(t!=null) {
            t.mapRect(outBounds);
        }
        if(item.getClass() == StopPoint.class) {
            outBounds.offset(-outBounds.width()/2, -outBounds.height()/2);
        }
    }

    private boolean checkSnap(RectF from, RectF to) {
        boolean snap = false;
        if((snap_what&SNAP_LEFT)!=0) {
            snap |= doesSnapX(from.left, to.left);
            snap |= doesSnapX(from.left, to.right);
        }
        if((snap_what&SNAP_RIGHT)!=0) {
            snap |= doesSnapX(from.right, to.right);
            snap |= doesSnapX(from.right, to.left);
        }
        if((snap_what&SNAP_CENTER)!=0) {
            snap |= doesSnapX(from.centerX(), to.centerX());
            snap |= doesSnapY(from.centerY(), to.centerY());
        }
        if((snap_what&SNAP_TOP)!=0) {
            snap |= doesSnapY(from.top, to.top);
            snap |= doesSnapY(from.top, to.bottom);
        }
        if((snap_what&SNAP_BOTTOM)!=0) {
            snap |= doesSnapY(from.bottom, to.bottom);
            snap |= doesSnapY(from.bottom, to.top);
        }

        return snap;
    }

    private boolean doesSnapX(float from, float to) {
        float diff = to-from;
        float abs_diff = Math.abs(diff);
        if(abs_diff<touch_slop && abs_diff<=Math.abs(min_dx)) {
            min_dx = diff;
            anchor_x = to;
            return true;
        } else {
            return false;
        }
    }

    private boolean doesSnapY(float from, float to) {
        float diff = to-from;
        float abs_diff = Math.abs(diff);
        if(abs_diff<touch_slop && abs_diff<=Math.abs(min_dy)) {
            min_dy = diff;
            anchor_y = to;
            return true;
        } else {
            return false;
        }
    }
}
