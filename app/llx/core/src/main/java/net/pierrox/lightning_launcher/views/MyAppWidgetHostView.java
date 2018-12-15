package net.pierrox.lightning_launcher.views;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;

/**
 * Hack, not all android versions allow several views for a single widget. Use a single views that "smartly" manages reparenting
 */
public final class MyAppWidgetHostView extends AppWidgetHostView {
    private ArrayList<View> mParents = new ArrayList<>(2);
    private ArrayList<View> mAllowedRedraws;

    public MyAppWidgetHostView(Context context) {
        super(context);
    }

    public void registerParent(View parent) {
        mParents.add(parent);
    }

    public void unregisterParent(View parent) {
        mParents.remove(parent);
        if(mAllowedRedraws != null) {
            mAllowedRedraws.remove(parent);
        }
        int size = mParents.size();
        if (size > 0) {
            mParents.get(size - 1).invalidate();
        }
    }

    public void moveParentToTop(View parent) {
        mParents.remove(parent);
        mParents.add(parent);
    }

    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        super.updateAppWidget(remoteViews);
        mAllowedRedraws = new ArrayList<>(mParents);
        for (View parent : mParents) {
            parent.invalidate();
        }
    }

    public boolean isReparentAllowed(View parent) {
        if (mParents.get(mParents.size() - 1) == parent) {
            return true;
        }
        if (mAllowedRedraws != null && mAllowedRedraws.contains(parent)) {
            mAllowedRedraws.remove(parent);
            if (mAllowedRedraws.size() == 0) {
                mAllowedRedraws = null;
            }
            return true;
        }
        return false;
    }
}
