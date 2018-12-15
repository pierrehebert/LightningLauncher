package net.pierrox.lightning_launcher.data;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.configuration.JsonFields;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.WidgetView;

import org.json.JSONException;
import org.json.JSONObject;

public class Widget extends Item {

	public static final int NO_APP_WIDGET_ID = -1;
	
	// mAppWidgetId is -1 for LL Widget
	private int mAppWidgetId=NO_APP_WIDGET_ID;
	private ComponentName mComponentName;
    private String mAppWidgetLabel;


	public Widget(Page page) {
		super(page);
	}

	protected void copyTo(Item item) {
		super.copyTo(item);
		Widget w=(Widget)item;
		w.mAppWidgetId=mAppWidgetId;
		w.mComponentName=mComponentName;
	}
	
	@Override
	public Item clone() {
		Widget w=new Widget(mPage);
		copyTo(w);
		return w;
	}
	
	public int getAppWidgetId() {
		return mAppWidgetId;
	}
	
	public void setAppWidgetId(int id) {
		mAppWidgetId=id;
	}

    public void setAppWidgetLabel(String label) {
        mAppWidgetLabel = label;
    }

    public String getAppWidgetLabel() {
        return mAppWidgetLabel;
    }

	public ComponentName getComponentName() {
		return mComponentName;
	}

	public void setComponentName(ComponentName cn) {
		mComponentName=cn;
	}

    public boolean hasConfigurationScreen() {
		AppWidgetManager app_widget_manager=AppWidgetManager.getInstance(LLApp.get());
		AppWidgetProviderInfo app_widget_info=app_widget_manager.getAppWidgetInfo(mAppWidgetId);
		if (app_widget_info!=null && app_widget_info.configure != null) {
			return true;
		} else {
			return false;
		}
    }

	public void onConfigure(Context context) {
		AppWidgetManager app_widget_manager=AppWidgetManager.getInstance(LLApp.get());
		AppWidgetProviderInfo app_widget_info=app_widget_manager.getAppWidgetInfo(mAppWidgetId);
		if (app_widget_info!=null && app_widget_info.configure != null) {
			Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			intent.setComponent(app_widget_info.configure);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);

			try {
				context.startActivity(intent);
			} catch(Exception e) {
				// pass
			}
		}
	}

	@Override
	public void onRemove(boolean keepResources) {
		super.onRemove(keepResources);
		if(!keepResources) {
			LLApp.get().getAppWidgetHost().deleteAppWidgetId(mAppWidgetId);
		}
	}

    @Override
	public void createFromJSONObject(JSONObject o) throws JSONException {
		readItemFromJSONObject(o);
		
		mAppWidgetId=o.optInt(JsonFields.WIDGET_APP_WIDGET_ID, NO_APP_WIDGET_ID);
		String cn=o.optString(JsonFields.WIDGET_COMPONENT_NAME, null);
		if(cn!=null) {
			mComponentName=ComponentName.unflattenFromString(cn);
		}
        mAppWidgetLabel =o.optString(JsonFields.WIDGET_LABEL, null);
	}
	
	public static Widget createStatic(Page page, int id, Rect cell_p, Rect cell_l, ComponentName cn, int app_widget_id) {
		Widget widget=new Widget(page);

        widget.init(id, cell_p, cell_l);

		widget.mComponentName=cn;
		widget.mAppWidgetId=app_widget_id;
        if(app_widget_id != NO_APP_WIDGET_ID) {
            AppWidgetProviderInfo app_widget_info = AppWidgetManager.getInstance(LLApp.get()).getAppWidgetInfo(app_widget_id);
            if(app_widget_info != null) {
                widget.mAppWidgetLabel = app_widget_info.label;
            }
        }

		return widget;
	}

	@Override
	public ItemView createView(Context context) {
		return new WidgetView(context, this);
	}
}
