package net.pierrox.lightning_launcher.prefs;

import android.content.Context;

import net.pierrox.lightning_launcher.data.ActionsDescription;
import net.pierrox.lightning_launcher.data.EventAction;

public class LLPreferenceEventAction extends LLPreference {

    private ActionsDescription mActions;

    public LLPreferenceEventAction(Context context, int id, int title, EventAction value, EventAction defaultValue, ActionsDescription actions) {
        super(id, context.getString(title), actions.getActionName(value.action), value, defaultValue);

        mActions = actions;

        updateLabel();
    }

    public LLPreferenceEventAction(Context context, int id, int title, ActionsDescription actions) {
        super(context, id, title, 0);

        mActions = actions;
    }

    public EventAction getValue() {
        return (EventAction) mValue;
    }

    @Override
    public void setValue(Object value, Object defaultValue) {
        super.setValue(value, defaultValue);
        updateLabel();
    }

    public void setValue(EventAction ea) {
        mValue = ea;
        updateLabel();
    }

    public ActionsDescription getActions() {
        return mActions;
    }

    private void updateLabel() {
        EventAction ea = getValue();
        if(ea == null || mActions == null) {
            setSummary("");
        } else {
            String more = ea.next == null ? "" : " (+)";
            setSummary(mActions.getActionName(ea.action) + more);
        }
    }
}
