package net.pierrox.lightning_launcher.views.item;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.widget.TextView;

import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Shortcut;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.views.IconLabelView;
import net.pierrox.lightning_launcher.views.IconView;
import net.pierrox.lightning_launcher.views.MyTextView;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;

public class ShortcutView extends ItemView {
    protected IconLabelView mIconLabelView;
    private Bitmap mHighlightBitmap;

    private int mStdIconWidth;
    private int mStdIconHeight;
    // ease of use, to avoid casts
    private Shortcut mShortcut;

    public ShortcutView(Context context, Item item, int std_icon_width, int std_icon_height) {
        super(context, item);

        mShortcut = (Shortcut) mItem;
        mStdIconWidth = std_icon_width;
        mStdIconHeight = std_icon_height;
    }

    @Override
    public void init() {
        ItemConfig ic = mItem.getItemConfig();
        ShortcutConfig sc = mShortcut.getShortcutConfig();
        mIconLabelView=new IconLabelView(getContext(), mShortcut.getLabel(), mStdIconWidth, mStdIconHeight, mShortcut.getSharedAsyncGraphicsDrawable(), ic, sc);
        setView(mIconLabelView);

        if(mResumed) {
            SharedAsyncGraphicsDrawable d = mShortcut.getSharedAsyncGraphicsDrawable();
            if(sc.iconSizeMode == ShortcutConfig.IconSizeMode.NORMALIZED) {
                d.setFilterBitmap(true);
            }
            if (d != null) {
                d.resume();
            }
        }
    }

    public Shortcut getShortcut() {
        return mShortcut;
    }

    @Override
    public void destroy() {
        super.destroy();

        if(isInitDone() && mIconLabelView != null) {
            mIconLabelView.destroy();
        }
    }

    @Override
    public void pause() {
        super.pause();

        SharedAsyncGraphicsDrawable d = mShortcut.getSharedAsyncGraphicsDrawable();
        if(d != null) {
            d.pause();
        }
    }

    @Override
    public void resume() {
        super.resume();

        SharedAsyncGraphicsDrawable d = mShortcut.getSharedAsyncGraphicsDrawable();
        if(d != null) {
            d.resume();
        }
    }

    public IconLabelView getIconLabelView() {
        return mIconLabelView;
    }

    public void updateLabelText() {
        if(mIconLabelView!=null) {
            TextView tv=mIconLabelView.getTextView();
            if(tv!=null) {
                tv.setText(mShortcut.getLabel());
            }
        }
    }

    public void updateLabelColor() {
        if(mIconLabelView!=null) {
            TextView tv=mIconLabelView.getTextView();
            if(tv!=null) {
                ShortcutConfig shortcutConfig = mShortcut.getShortcutConfig();
                // highlighted: higher priority, then focused, then normal
                tv.setTextColor(mHighlighted ? shortcutConfig.selectionColorLabel : (mFocused ? shortcutConfig.focusColorLabel : shortcutConfig.labelFontColor));
            }
        }
    }

    public void updateLabelFontSize() {
        if(mIconLabelView!=null) {
            MyTextView tv = mIconLabelView.getTextView();
            if (tv != null) {
                tv.setTextSize(mShortcut.getShortcutConfig().labelFontSize);
            }
        }
    }

    public void updateLabelFontStyle() {
        if(mIconLabelView!=null) {
            MyTextView tv = mIconLabelView.getTextView();
            if (tv != null) {
                mShortcut.getShortcutConfig().applyFontStyleToTextView(tv);
            }
        }
    }

    public void updateLabelVisibility() {
        if(mIconLabelView!=null) {
            mIconLabelView.updateLabelVisibility(mShortcut.getLabel());
        }
    }

    public void updateIconVisibility() {
        if(mIconLabelView!=null) {
            mIconLabelView.updateIconVisibility(mShortcut.getSharedAsyncGraphicsDrawable());
        }
    }

    public void updateIconGraphics() {
        IconView iv = mIconLabelView.getIconView();
        if(iv != null) {
            SharedAsyncGraphicsDrawable d = mShortcut.getSharedAsyncGraphicsDrawable();
            if(mResumed) {
                d.pause();
            }

            mShortcut.recreateSharedAsyncGraphicsDrawable();
            d = mShortcut.getSharedAsyncGraphicsDrawable();

            int iconSize = Utils.getStandardIconSize();
            int std_icon_width = mShortcut.computeTotalWidth(iconSize);
            int std_icon_height = mShortcut.computeTotalHeight(iconSize);
            mIconLabelView.updateSharedAsyncGraphicsDrawable(d, std_icon_width, std_icon_height);

            if(mResumed) {
                d.resume();
            }
        }
    }

    public Bitmap getHighlightBitmap() {
        return mHighlightBitmap;
    }

    public void highlightText(String text) {
        if(!isInitDone()) {
            return;
        }
        TextView tv = mIconLabelView.getTextView();
        if(tv != null) {
            String label = mShortcut.getLabel();
            if(text == null) {
                tv.setText(label);
            } else {
                Spannable t = new SpannableStringBuilder(label);
                BackgroundColorSpan hl_color = new BackgroundColorSpan(0x80808080);
                int start = label.toLowerCase().indexOf(text);
                t.setSpan(hl_color, start, start+text.length(), 0);
                tv.setText(t);
            }
        }
    }

    protected Bitmap createHighlightBitmap() {
        ItemConfig ic = mItem.getItemConfig();
        if(ic.selectionEffect != ItemConfig.SelectionEffect.HOLO) {
            return null;
        }

        if(mHighlightBitmap != null) {
            return mHighlightBitmap;
        }

        if(mIconLabelView == null) {
            return null;
        }

        try {
            final int padding = mOutlineHelper.mMaxOuterBlurRadius;
            final Bitmap b = Bitmap.createBitmap(mIconLabelView.getWidth() + padding*2, mIconLabelView.getHeight() + padding*2, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas();
            canvas.setBitmap(b);
            View tv = mIconLabelView.getTextView();
            if(tv != null) tv.setVisibility(View.INVISIBLE);
            canvas.translate(padding, padding);
            mIconLabelView.draw(canvas);
            canvas.translate(-padding, -padding);
            if(tv != null) tv.setVisibility(View.VISIBLE);
            int color = ic.box.ccs;
            mOutlineHelper.applyExtraThickExpensiveOutlineWithBlur(b, canvas, color, color);

            return b;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void setHighlightedNow(boolean highlighted) {
        super.setHighlightedNow(highlighted);
        updateLabelColor();
        mHighlightBitmap = highlighted ? createHighlightBitmap() : null;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        updateLabelColor();
    }

    @Override
    public void prepareDraggedBitmap() {
        View tv = mIconLabelView == null ? null : mIconLabelView.getTextView();
        if(tv != null) tv.setVisibility(View.INVISIBLE);
        super.prepareDraggedBitmap();
        if(tv != null) tv.setVisibility(View.VISIBLE);
    }

    @Override
    public void updateViewSize() {
        super.updateViewSize();
        ShortcutConfig sc = mShortcut.getShortcutConfig();
        if(sc.iconSizeMode != ShortcutConfig.IconSizeMode.STANDARD && sc.iconSizeMode != ShortcutConfig.IconSizeMode.NORMALIZED) {
            final SharedAsyncGraphicsDrawable d = mShortcut.getSharedAsyncGraphicsDrawable();
            if (d != null && d.getType() != SharedAsyncGraphicsDrawable.TYPE_SVG) {
                d.loadGraphicsAsync();
            }
        }
    }
}
