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
