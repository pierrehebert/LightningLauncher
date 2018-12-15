package net.pierrox.lightning_launcher.configuration;

import net.pierrox.lightning_launcher.data.JsonLoader;
import org.json.JSONException;
import org.json.JSONObject;

public class DynamicTextConfig extends JsonLoader {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    public enum Source {
        UNREAD_SMS,
        UNREAD_GMAIL,
        MISSED_CALLS,
        DATE,
        STORAGE,
        BATTERY_LEVEL,
        HEAP_MAX,
        HEAP_FREE
    }

    public enum StorageSource {
        INTERNAL,
        EXTERNAL
    }

    public enum StorageFormat {
        NORMAL,
        SHORT,
        PERCENT,
        BYTES
    }

    public enum StorageWhat {
        LEFT,
        USED,
        CAPACITY
    }

    public Source source = Source.DATE;

    public boolean displayEmpty = true;
    public String dateFormat = DEFAULT_DATE_FORMAT;
    public String countFormat = "0";
    public String textFormat = "%s";
    public String gmailLabel = null;
    public StorageSource storageSource = StorageSource.INTERNAL;
    public StorageFormat storageFormat = StorageFormat.NORMAL;
    public StorageWhat storageWhat = StorageWhat.LEFT;

    public static DynamicTextConfig readFromJsonObject(JSONObject o, DynamicTextConfig d) throws JSONException {
        DynamicTextConfig c=new DynamicTextConfig();

        c.loadFieldsFromJSONObject(o, d);

        return c;
    }

    public DynamicTextConfig clone() {
        DynamicTextConfig dtc = new DynamicTextConfig();
        dtc.copyFrom(this);
        return dtc;
    }
}
