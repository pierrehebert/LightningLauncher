package net.pierrox.lightning_launcher.configuration;

import android.os.Build;

import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.EventAction;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.engine.variable.Binding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class ItemConfig extends JsonLoader {
    public enum PinMode {
        NONE,
        XY,
        X,
        Y
    }

    public enum SelectionEffect {
        PLAIN,
        HOLO,
        MATERIAL
    }

	public enum LaunchAnimation {
		NONE,
		FADE,
		SYSTEM,
		SLIDE_UP,
		SLIDE_DOWN,
		SLIDE_LEFT,
		SLIDE_RIGHT,
		SCALE_CENTER
	}

    public boolean onGrid=true;

	public String box_s;
	public boolean filterTransformed=true;
	public PinMode pinMode=PinMode.NONE;
	public int alpha=255;
	public boolean enabled=true;
    public SelectionEffect selectionEffect = Build.VERSION.SDK_INT >=21 ? SelectionEffect.MATERIAL : SelectionEffect.HOLO;
    public boolean selectionEffectMask = true;
	public boolean rotate=false;
    public boolean hardwareAccelerated=true;

	public Box box;
	
	public EventAction swipeLeft = EventAction.UNSET();
	public EventAction swipeRight = EventAction.UNSET();
	public EventAction swipeUp = EventAction.UNSET();
	public EventAction swipeDown = EventAction.UNSET();
    public EventAction tap = EventAction.UNSET();
	public EventAction longTap = EventAction.UNSET();
	public EventAction touch = EventAction.UNSET();
	public EventAction paused = EventAction.UNSET();
	public EventAction resumed = EventAction.UNSET();
	public EventAction menu = EventAction.UNSET();

    public Binding[] bindings = null;

	public LaunchAnimation launchAnimation = LaunchAnimation.SYSTEM;

	public ItemConfig() {
		box=new Box();
	}

    private static final String sPinned = "pinned";
	@Override
	public void loadFieldsFromJSONObject(JSONObject json, Object d) {
		super.loadFieldsFromJSONObject(json, d);
        if(json.has(sPinned)) {
            // compat, pinned will be defined only if not false
            pinMode=PinMode.XY;
        }
		ItemConfig ic_d = (ItemConfig)d;
		if(box_s == null) {
			box_s = ic_d.box.toString(null);
		}
		box.loadFromString(box_s, ic_d.box);
		box.bgNormal = ic_d.box.bgNormal;
		box.bgSelected = ic_d.box.bgSelected;
		box.bgFocused = ic_d.box.bgFocused;
	}
	
	public void copyFrom(ItemConfig o) {
		super.copyFrom(o);
		box=new Box();
		box.loadFromString(o.box.toString(box), box);
        box.bgNormal = o.box.bgNormal;
        box.bgSelected = o.box.bgSelected;
        box.bgFocused = o.box.bgFocused;
	}
	
	public static ItemConfig readFromJsonObject(JSONObject o, ItemConfig d) throws JSONException {
		ItemConfig c=new ItemConfig();
		
		c.loadFieldsFromJSONObject(o, d);
		
		return c;
	}

    public void loadAssociatedIcons(File icon_dir, int id) {
        box.loadAssociatedDrawables(icon_dir, id, true);
    }
}