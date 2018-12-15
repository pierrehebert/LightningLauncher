package net.pierrox.lightning_launcher.data;

import android.content.Context;
import android.graphics.Rect;

import net.pierrox.lightning_launcher.views.item.CustomViewView;
import net.pierrox.lightning_launcher.views.item.ItemView;

import org.json.JSONException;
import org.json.JSONObject;

public class CustomView extends Item {

    // public properties
    public String onCreate;
    public String onDestroy;

    public CustomView(Page page) {
        super(page);
    }

    @Override
    public void init(int id, Rect cell_p, Rect cell_l) {
        super.init(id, cell_p, cell_l);
    }

    @Override
    public void createFromJSONObject(JSONObject o) throws JSONException {
        readItemFromJSONObject(o);

        JsonLoader.loadFieldsFromJSONObject(this, o, null);
    }

    @Override
    public Item clone() {
        return null;
    }

    @Override
    public ItemView createView(Context context) {
        return new CustomViewView(context, this);
    }
}
