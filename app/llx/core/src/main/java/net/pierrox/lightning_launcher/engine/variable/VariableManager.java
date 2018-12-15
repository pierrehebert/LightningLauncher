package net.pierrox.lightning_launcher.engine.variable;

import android.os.Handler;
import android.util.Pair;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.script.ScriptManager;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.api.Lightning;
import net.pierrox.lightning_launcher.script.api.Property;
import net.pierrox.lightning_launcher.script.api.PropertyEditor;
import net.pierrox.lightning_launcher.script.api.PropertySet;
import net.pierrox.lightning_launcher.views.item.ItemView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.RhinoException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class VariableManager {
    private LightningEngine mEngine;
    private int mDate;
    private HashMap<String,Variable> mVariables;
    private HashMap<String, ArrayList<Target>> mVariableTargets;
    private HashSet<Variable> mModifiedVariables;
    private StringBuilder mTmpStringBuilder = new StringBuilder();
    private boolean mUpdatesPaused;

    public static Pair<String,? extends Interpolator> DEFAULT_INTERPOLATOR = new Pair<>("ad", new AccelerateDecelerateInterpolator());
    private static Pair<String,Interpolator>[] sInterpolators = new Pair[] {
        DEFAULT_INTERPOLATOR,
        new Pair<>("ac", new AccelerateInterpolator()),
        new Pair<>("de", new DecelerateInterpolator()),
        new Pair<>("li", new LinearInterpolator()),
        new Pair<>("an", new AnticipateInterpolator()),
        new Pair<>("as", new AnticipateOvershootInterpolator()),
        new Pair<>("bo", new BounceInterpolator()),
        new Pair<>("ov", new OvershootInterpolator()),
    };

    private static class Animator {
        Interpolator interpolator;
        long start;
        long duration;
        long offset;
        double fromValue;
        double toValue;
        String name;
        String varName;
        double lastValue;
        int lastValueDate = -1;
        boolean done;

        private Animator(String var_name, long duration, String interpolator_name, long offset) {
            this.name = name(var_name, duration, interpolator_name, offset);
            this.varName = var_name;
            this.duration = duration;
            this.interpolator = getInterpolatorByName(interpolator_name);
            this.offset = offset;
        }

        public void start(double fromValue, double toValue, int date) {
            this.fromValue = this.lastValue = fromValue;
            this.toValue = toValue;
            this.lastValueDate = date;
            this.start = AnimationUtils.currentAnimationTimeMillis();
            this.done = false;
        }

        public void setLastValue(double value, int date) {
            lastValue = value;
            lastValueDate = date;
        }

        private Interpolator getInterpolatorByName(String interpolator_name) {
            for(Pair<String,Interpolator> p : sInterpolators) {
                if(p.first.equals(interpolator_name)) {
                    return p.second;
                }
            }

            return DEFAULT_INTERPOLATOR.second;
        }

        public static String name(String var_name, long duration, String interpolator, long offset) {
            return var_name+duration+interpolator+offset;
        }
    }

    private Handler mHandler;
    private ArrayList<Animator> mAnimators;
    private boolean mAnimateScheduled;
    private File mValuesFile;

    private static final String TOK_VARIABLES = "v";
    private static final String TOK_NAME = "n";
    private static final String TOK_VALUE = "v";

    public VariableManager(LightningEngine engine, File load_values_from) {
        mEngine = engine;
        mVariables = new HashMap<>();
        mVariableTargets = new HashMap<>();
        mHandler = new Handler();
        mAnimators = new ArrayList<>();

        mValuesFile = load_values_from;
        JSONObject values = FileUtils.readJSONObjectFromFile(load_values_from);
        if(values != null) {
            try {
                JSONArray jvariables = values.getJSONArray(TOK_VARIABLES);
                int l = jvariables.length();
                for(int i=0; i<l; i++) {
                    JSONObject jvariable = jvariables.getJSONObject(i);
                    String name = jvariable.getString(TOK_NAME);
                    Object value = jvariable.get(TOK_VALUE);
                    mVariables.put(name, new Variable(name, value));
                }
            } catch (JSONException e) {
                // pass
            }
        }
    }

    public void saveValues() {
        try {
            JSONArray jvariables = new JSONArray();
            for(Variable variable : getUserVariables()) {
                if(variable.value != null) {
                    JSONObject jvariable = new JSONObject();
                    jvariable.put(TOK_NAME, variable.name);
                    jvariable.put(TOK_VALUE, variable.value);
                    jvariables.put(jvariable);
                }
            }
            JSONObject jdata = new JSONObject();
            jdata.put(TOK_VARIABLES, jvariables);
            FileUtils.saveStringToFile(jdata.toString(), mValuesFile);
        } catch(Exception e) {
            mValuesFile.delete();
        }
    }

    public void pauseUpdates() {
        mUpdatesPaused = true;
    }

    public void resumeUpdates() {
        mUpdatesPaused = false;
        if(mModifiedVariables != null && mModifiedVariables.size()>0) {
            commit();
        }
    }

    public Variable getVariable(String name) {
        Variable var = mVariables.get(name);
        if(var == null) {
            var = new Variable(name, null);
            mVariables.put(name, var);
        }
        return var;
    }

    public Collection<Variable> getAllVariables() {
        return mVariables.values();
    }

    public ArrayList<Variable> getUserVariables() {
        LLApp llApp = LLApp.get();
        Pair<String,BuiltinVariable[]>[] builtin_variables = mEngine.getBuiltinDataCollectors().getBuiltinVariables();
        Collection<Variable> all_variables = getAllVariables();
        ArrayList<Variable> user_variables = new ArrayList<>();
        for(Variable variable : all_variables) {
            boolean found = false;
            String name = variable.name;
            loop:
            for(Pair<String,BuiltinVariable[]> p : builtin_variables) {
                for(BuiltinVariable bv : p.second) {
                    if(bv.name.equals(name)) {
                        found = true;
                        break loop;
                    }
                }
            }
            if(!found) {
                user_variables.add(variable);
            }
        }
        Collections.sort(user_variables, new Comparator<Variable>() {
            @Override
            public int compare(Variable lhs, Variable rhs) {
                return lhs.name.compareToIgnoreCase(rhs.name);
            }
        });

        return user_variables;
    }

    public void edit() {
        if(mModifiedVariables == null) {
            mModifiedVariables = new HashSet<>();
        }
        mDate++;
    }

    public void setVariable(String name, Object new_value) {
        Variable variable = getVariable(name);
        Object old_value = variable.value;
        if((new_value==null && old_value != null) || (new_value != null && !new_value.equals(old_value))) {
            for(int i=mAnimators.size()-1; i>=0; i--) {
                Animator animator = mAnimators.get(i);
                if(animator.varName.equals(name)) {
                    animator.start(animator.done ? Value.asDouble(old_value) : animator.lastValue, Value.asDouble(new_value), mDate);
                }
            }

            variable.value = new_value;
            mModifiedVariables.add(variable);
        }
    }

    public Object animateVariable(String var_name, int duration, String interpolator, int offset) {
        Variable variable = getVariable(var_name);
        Object new_value = variable.value;

        double result;
        boolean animate;
        String animator_name = Animator.name(var_name, duration, interpolator, offset);
        Animator animator = findAnimator(animator_name);
        if(animator == null) {
            // prepare an animator but do not run it, it will be activated upon next variable change
            double to = Value.asDouble(new_value);

            animator = new Animator(var_name, duration, interpolator, offset);
            animator.start(to, to, mDate);
            animator.done = true;
            mAnimators.add(animator);
            result = to;
            animate = false;
        } else {
            if(animator.lastValueDate == mDate || animator.done) {
                result = animator.lastValue;
                animate = true;
            } else {
                long delta = AnimationUtils.currentAnimationTimeMillis() - animator.start;
                if(delta <= animator.offset) {
                    result = animator.fromValue;
                    animate = true;
                } else {
                    delta -= animator.offset;
                    long d = animator.duration;

                    if (delta >= d) {
                        result = animator.toValue;
                        animator.done = true;
                        animate = false;
                    } else {
                        float v = delta / (float) d;
                        result = animator.fromValue + (animator.toValue - animator.fromValue) * animator.interpolator.getInterpolation(v);
                        animate = true;
                    }
                }
            }
        }
        if(animate && !mAnimateScheduled) {
            mAnimateScheduled = true;
            mHandler.post(mAnimate);
        }
        animator.setLastValue(result, mDate);
        return result;
    }

    private Animator findAnimator(String name) {
        for(Animator a : mAnimators) {
            if(name.equals(a.name)) {
                return a;
            }
        }

        return null;
    }

    private Runnable mAnimate = new Runnable() {
        @Override
        public void run() {
            edit();
            for(Animator a : mAnimators) {
                mModifiedVariables.add(getVariable(a.varName));
            }
            commit();
            int done_count = 0;
            for(int i=mAnimators.size()-1; i>=0; i--) {
                Animator a = mAnimators.get(i);
                if(a.done) {
                    done_count++;
                }
            }
            if(mAnimators.size() > done_count) {
                mHandler.post(mAnimate);
            } else {
                mAnimateScheduled = false;
            }
        }
    };

    private ArrayList<Target> mTmpModifiedTargets = new ArrayList<>(); // limit allocations
    public void commit() {
        if(!mUpdatesPaused) {
            if(mTmpModifiedTargets == null) {
                mTmpModifiedTargets = new ArrayList<>();
            } else {
                mTmpModifiedTargets.clear();
            }

            for (Variable variable : mModifiedVariables) {
                ArrayList<Target> targets = getTargetsForVariable(variable.name);
                // during traversal, the array can be modified if a binding triggers a script which
                // modifies the item and forces the view to be rebuilt, and as a consequence bindings
                // to be rebuilt. This should be safe since the number and order of bindings remain
                // the same. It is forbidden to modify bindings in a script triggered by a binding.
                for (int i=targets.size()-1; i>=0; i--) {
                    Target target = targets.get(i);
                    if (target.dateComputed != mDate) {
                        Object old_value = target.value;
                        computeTarget(target, target.itemView.getParentItemLayout().getScreen());
                        Object new_value = target.value;
                        if ((old_value == null && new_value != null) || (old_value != null && !old_value.equals(new_value))) {
                            mTmpModifiedTargets.add(target);
                        }
                    }
                }
            }

            applyTargets(mTmpModifiedTargets);

            mModifiedVariables.clear();
        }
    }

    private void applyTargets(ArrayList<Target> targets) {
        // TODO sort targets per page/item in order to group properties changes
        ScriptExecutor se = mEngine.getScriptExecutor();
        Lightning ll = se.getLightning();
        for(Target target : targets) {
            PropertySet ps = ll.getCachedItem(target.itemView).getProperties();
            Object value = target.value;
            if(value != null) {
                String field = target.field;
                PropertyEditor editor = ps.edit();
                try {
                    switch(Property.getType(field)) {
                        case Property.TYPE_BOOLEAN: editor.setBoolean(field, Value.asBoolean(value)); break;
                        case Property.TYPE_INTEGER: editor.setInteger(field, Value.asInteger(value)); break;
                        case Property.TYPE_FLOAT: editor.setFloat(field, Value.asFloat(value)); break;
                        case Property.TYPE_STRING: editor.setString(field, Value.asString(value)); break;
                    }
                } catch(RhinoException e) {
                    se.displayScriptError(e);
                    return;
                }
                editor.commit();
            }
        }
    }

    public Pair<String,String[]> convertFormulaToScript(String formula) {
        int formula_length = formula.length();

        // decode variable names
        HashSet<String> names = new HashSet<>();
        int p = -1;
        while((p = formula.indexOf('$', p+1)) != -1) {
            p++;
            if(p < formula_length && isIdentifierChar(formula.charAt(p))) {
                String identifier = getIdentifier(formula, p);
                if(identifier != null) {
                    names.add(identifier);
                }
            }
        }
        int variable_count = names.size();
        String[] variable_names = new String[variable_count];
        names.toArray(variable_names);

        mTmpStringBuilder.setLength(0);
        if(!formula.contains("return")) {
            mTmpStringBuilder.append("return ");
        }
        for(p=0; p<formula_length; p++) {
            char c = formula.charAt(p);
            if(c == '$') {
                p++;
                if(p == formula_length) {
                    break;
                }
                c = formula.charAt(p);
            }
            mTmpStringBuilder.append(c);
        }

        return new Pair<>(mTmpStringBuilder.toString(), variable_names);
    }

    public void updateBindings(ItemView itemView, Binding[] bindings, boolean apply, Screen fromScreen, boolean removeOldTargets) {
        // remove old targets
        if(removeOldTargets) {
            ScriptManager sm = mEngine.getScriptManager();
            for (ArrayList<Target> targets : mVariableTargets.values()) {
                for (int l = targets.size() - 1; l >= 0; l--) {
                    Target target = targets.get(l);
                    if (target.itemView == itemView) {
                        if (target.script != null) {
                            sm.deleteScript(target.script);
                        }
                        targets.remove(l);
                    }
                }
            }
        }

        if(bindings == null) {
            return;
        }

        // add new one
        ScriptManager sm = mEngine.getScriptManager();
        ArrayList<Target> targets = new ArrayList<>(bindings.length);
        for(Binding binding : bindings) {
            if(!binding.enabled) {
                continue;
            }

            final String formula = binding.formula;
            int formula_length = formula.length();
            if(formula_length == 0) {
                continue;
            }

            final String field = binding.target;
            if(field == null || field.length() == 0) {
                continue;
            }

            Target target;

            String simple_identifier = null;
            if (formula.charAt(0) == '$') {
                String identifier = getIdentifier(formula, 1);
                if (identifier != null && identifier.length() + 1 == formula_length) {
                    simple_identifier = identifier;
                }
            }
            if (simple_identifier == null) {
                // this is a function, convert it to a script and extract variable names
                Pair<String,String[]> p = convertFormulaToScript(formula);

                // create a script from this
                Script script = sm.createScriptForBinding(itemView, binding);
                script.setProcessedText(p.first);

                target = addTarget(itemView, field, p.second, script);
            } else {
                // this is a single variable
                target = addTarget(itemView, field, new String[]{simple_identifier}, null);
            }

            if(apply) {
                computeTarget(target, fromScreen);
                targets.add(target);
            }
        }

        // apply new values
        if(apply) {
            applyTargets(targets);
        }
    }

    private Target addTarget(ItemView itemView, String field, String[] variable_names, Script script) {
        int l = variable_names.length;
        Variable[] variables = new Variable[l];
        for(int i=0; i<l; i++) {
            variables[i] = getVariable(variable_names[i]);
        }

        Target target = new Target(itemView, field, variables, script);
        for(Variable v : variables) {
            getTargetsForVariable(v.name).add(target);
        }

        return target;
    }

    private static String getIdentifier(String text, int start) {
        int pos = start+1;
        int l = text.length();
        while(pos < l && isIdentifierChar(text.charAt(pos))) {
            pos++;
        }
        return pos > l ? null : text.substring(start, pos);
    }

    private static boolean isIdentifierChar(char c) {
        return c=='_' || Character.isLetterOrDigit(c);
    }

    private ArrayList<Target> getTargetsForVariable(String name) {
        ArrayList<Target> targets = mVariableTargets.get(name);
        if(targets == null) {
            targets = new ArrayList<>();
            mVariableTargets.put(name, targets);
        }
        return targets;
    }

    private void computeTarget(Target target, Screen fromScreen) {
        if(target.script == null) {
            target.value = target.variables[0].value;
        } else {
            int l = target.variables.length;
            Object[] arguments = new Object[l+2];
            mTmpStringBuilder.setLength(0);
            mTmpStringBuilder.append("item,target");

            // TODO not sure whether this is really efficient, can it be optimized?
            ScriptExecutor se = mEngine.getScriptExecutor();
            Lightning ll = se.getLightning();
            arguments[0] =  ll.getCachedItem(target.itemView);
            arguments[1] =  target.field;

            for(int i=0; i<l; i++) {
                Variable variable = target.variables[i];
                arguments[i+2] = variable.value;
                mTmpStringBuilder.append(',');
                mTmpStringBuilder.append(variable.name);
            }
            Object result = se.runScriptAsFunction(fromScreen, target.script.id, mTmpStringBuilder.toString(), arguments, false, true);
            if(result != null) {
                target.value = result;
            }
        }
        target.dateComputed = mDate;
    }
}
