/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

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
