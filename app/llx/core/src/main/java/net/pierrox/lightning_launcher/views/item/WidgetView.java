package net.pierrox.lightning_launcher.views.item;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.data.Widget;
import net.pierrox.lightning_launcher.views.MyAppWidgetHostView;

import java.io.File;
import java.io.FileInputStream;

public class WidgetView extends ItemView {

    private View mWidgetView;
    private boolean mIsGood;


    public WidgetView(Context context, Widget widget) {
        super(context, widget);
    }

    @Override
    public void init() {
        Context context = getContext();

        Widget widget = (Widget) mItem;

        View v=null;

        ComponentName componentName = widget.getComponentName();
        String appWidgetLabel = widget.getAppWidgetLabel();

        int mAppWidgetId = widget.getAppWidgetId();
        AppWidgetManager app_widget_manager=AppWidgetManager.getInstance(LLApp.get());
        AppWidgetProviderInfo app_widget_info=app_widget_manager.getAppWidgetInfo(mAppWidgetId);

        if(app_widget_info != null) {
            AppWidgetHostView app_widget_host_view=LLApp.get().getAppWidgetHost().getWidgetView(context, mAppWidgetId, app_widget_info);
            app_widget_host_view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            app_widget_host_view.setAppWidget(mAppWidgetId, app_widget_info);
            // used to store last widget size, in order to filter calls to updateAppWidgetHostViewSize
            app_widget_host_view.setTag(R.id.w_w, Integer.valueOf(0));
            app_widget_host_view.setTag(R.id.w_h, Integer.valueOf(0));
            //	        app_widget_host_view.setDrawingCacheEnabled(true);
            v=app_widget_host_view;
        }

        if(v==null) {
            FrameLayout fl = new FrameLayout(context);


            Bitmap widget_preview;
            try {
                File icon_file = widget.getDefaultIconFile();
                widget_preview = BitmapFactory.decodeStream(new FileInputStream(icon_file));
            } catch (Throwable e) {
                widget_preview = null;
            }
            if(widget_preview != null) {
                ImageView iv = new ImageView(context);
                iv.setImageBitmap(widget_preview);
                iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                fl.addView(iv);
            }

            TextView tv=new TextView(context);
            String text = context.getString(R.string.widget_e);
            if(appWidgetLabel != null) {
                text += "\n"+appWidgetLabel;
            }
            if(componentName != null) {
                text += "\n("+componentName.getPackageName()+")";
            }
            tv.setText(text);
            //tv.setTextSize(20);
            tv.setTextColor(Color.WHITE);
            tv.setGravity(Gravity.CENTER);
            tv.setShadowLayer(1, 0, 0, Color.BLACK);
            tv.setBackgroundColor(0x60000000);
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            fl.addView(tv);

            //fl.setBackgroundResource(android.R.drawable.alert_light_frame);
            fl.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            v=fl;

            mIsGood = false;
        } else {
            mIsGood = true;
        }

        mWidgetView = v;

        if(mWidgetView instanceof MyAppWidgetHostView) {
            ((MyAppWidgetHostView)mWidgetView).registerParent(this);
        }

        setView(v);
    }

    public boolean isGood() {
        return mIsGood;
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(checkWidgetViewParent()) {
            ((MyAppWidgetHostView)mWidgetView).moveParentToTop(this);
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void destroy() {
        super.destroy();
        if(mWidgetView instanceof MyAppWidgetHostView) {
            ((MyAppWidgetHostView)mWidgetView).unregisterParent(this);
        }
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        if(mWidgetView instanceof MyAppWidgetHostView) {
            MyAppWidgetHostView widgetView = (MyAppWidgetHostView) mWidgetView;
            if(widgetView.isReparentAllowed(this)) {
                checkWidgetViewParent();
            }
        }

        super.dispatchDraw(canvas);
    }

    private boolean checkWidgetViewParent() {
        // horrible hack : on some devices there can be only one widget view, so share it between items
        ViewParent parent = mWidgetView.getParent();
        if(parent != mSensibleView) {
            ((ViewGroup)parent).removeView(mWidgetView);
            mSensibleView.addView(mWidgetView);
            return true;
        }
        return false;
    }
}
