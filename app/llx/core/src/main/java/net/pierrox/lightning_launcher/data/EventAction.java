package net.pierrox.lightning_launcher.data;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Pair;

import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.variable.Variable;
import net.pierrox.lightning_launcher.script.Script;

import java.net.URISyntaxException;
import java.text.DecimalFormat;

public class EventAction {
    public int action;
    public String data;
    public EventAction next;

    public static EventAction UNSET() {
    	return new EventAction(GlobalConfig.UNSET, null);
    }
    
    public static final EventAction NOTHING() {
    	return new EventAction(GlobalConfig.NOTHING, null);
    }

    public EventAction() {
        // empty constructor for serialization
    }

    public EventAction(int action, String data) {
        this.action = action;
        this.data = data;
    }

    public EventAction(int action, String data, EventAction next) {
        this.action = action;
        this.data = data;
        this.next = next;
    }

    public EventAction clone() {
        return new EventAction(action, data, next==null ? null : next.clone());
    }

    public boolean equals(Object o) {
        if(o == null) return false;
        if(o.getClass() != EventAction.class) return false;
        EventAction ea = (EventAction)o;
        if(this.action != ea.action) return false;
        if((this.data == null && ea.data != null) || (this.data != null && !this.data.equals(ea.data))) return false;
        if((this.next == null && ea.next != null) || (this.next != null && !this.next.equals(ea.next))) return false;
        return true;
    }

    public String describe(LightningEngine engine) {
        if(data != null) {
            switch (action) {
                case GlobalConfig.RUN_SCRIPT:
                    Pair<Integer, String> idData = Script.decodeIdAndData(data);
                    if (idData != null) {
                        Script script = engine.getScriptManager().getOrLoadScript(idData.first);
                        if (script != null) {
                            return script.name;
                        }
                    }
                    break;

                case GlobalConfig.LAUNCH_APP:
                case GlobalConfig.LAUNCH_SHORTCUT:
                    try {
                        Intent intent = Intent.parseUri(data, 0);
                        PackageManager packageManager = engine.getContext().getPackageManager();
                        ResolveInfo activity = packageManager.resolveActivity(intent, 0);
                        if (activity != null) {
                            return activity.loadLabel(packageManager).toString();
                        }
                    } catch (URISyntaxException e) {
                        // pass
                    }
                    break;

                case GlobalConfig.GO_DESKTOP_POSITION:
                    try {
                        Intent intent = Intent.parseUri(data, 0);
                        int p = intent.getIntExtra(LightningIntent.INTENT_EXTRA_DESKTOP, Page.FIRST_DASHBOARD_PAGE);
                        Page page = engine.getOrLoadPage(p);
                        String description = Utils.formatPageName(page, page.findFirstOpener());
                        if(intent.hasExtra(LightningIntent.INTENT_EXTRA_X)) {
                            float x = intent.getFloatExtra(LightningIntent.INTENT_EXTRA_X, 0);
                            float y = intent.getFloatExtra(LightningIntent.INTENT_EXTRA_Y, 0);
                            float s = intent.getFloatExtra(LightningIntent.INTENT_EXTRA_SCALE, 1);
                            boolean absolute = intent.getBooleanExtra(LightningIntent.INTENT_EXTRA_ABSOLUTE, true);
                            DecimalFormat df = new DecimalFormat("0.##");
                            description += absolute ? " @" : " +";
                            description += df.format(x)+"x"+df.format(y)+"/"+df.format(s);
                        }
                        return description;
                    } catch (URISyntaxException e) {
                        // pass
                    }
                    break;

                case GlobalConfig.OPEN_FOLDER:
                    try {
                        int folderPage = Integer.parseInt(data);
                        Page page = engine.getOrLoadPage(folderPage);
                        return Utils.formatPageName(page, page.findFirstOpener());
                    } catch (NumberFormatException e) {
                        // pass
                    }
                    break;

                case GlobalConfig.SET_VARIABLE:
                    Variable v = Variable.decode(data);
                    if(v != null) {
                        return v.describe();
                    }
                    break;
            }
        }

        return null;
    }
}
