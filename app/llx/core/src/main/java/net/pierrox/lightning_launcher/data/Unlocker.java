package net.pierrox.lightning_launcher.data;

import android.content.Context;

import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.UnlockerView;

import org.json.JSONException;
import org.json.JSONObject;

public class Unlocker extends Item {
    public Unlocker(Page page) {
        super(page);
    }

    @Override
    public Item clone() {
        return new Unlocker(mPage);
    }

    @Override
    public ItemView createView(Context context) {
        return new UnlockerView(context, this);
    }

    @Override
    public void createFromJSONObject(JSONObject o) throws JSONException {
        readItemFromJSONObject(o);
    }


}
