package net.pierrox.lightning_launcher.script;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.faendir.rhino_android.RhinoAndroidHelper;

import net.dinglisch.android.tasker.ActionCodes;
import net.dinglisch.android.tasker.TaskerIntent;
import net.margaritov.preference.colorpicker.ColorPickerDialog;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.activities.MultiPurposeTransparentActivity;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.prefs.DialogPreferenceSlider;
import net.pierrox.lightning_launcher.script.api.Android;
import net.pierrox.lightning_launcher.script.api.Binding;
import net.pierrox.lightning_launcher.script.api.EventHandler;
import net.pierrox.lightning_launcher.script.api.Image;
import net.pierrox.lightning_launcher.script.api.ImageAnimation;
import net.pierrox.lightning_launcher.script.api.ImageBitmap;
import net.pierrox.lightning_launcher.script.api.ImageNinePatch;
import net.pierrox.lightning_launcher.script.api.ImageSvg;
import net.pierrox.lightning_launcher.script.api.LL;
import net.pierrox.lightning_launcher.script.api.Lightning;
import net.pierrox.lightning_launcher.script.api.Menu;
import net.pierrox.lightning_launcher.script.api.Property;
import net.pierrox.lightning_launcher.script.api.RectL;
import net.pierrox.lightning_launcher.util.AnimationDecoder;
import net.pierrox.lightning_launcher.engine.variable.Value;
import net.pierrox.lightning_launcher.engine.variable.VariableManager;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.views.Graphics;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;
import net.pierrox.android.lsvg.SvgDrawable;
import net.pierrox.lightning_launcher.views.item.ItemView;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;
import java.util.HashMap;

public class ScriptExecutor {
    private LightningEngine mEngine;
    private Handler mHandler;

    private Lightning mScriptLightning;
    private LL mScriptLL;
    private Android mScriptAndroid;
    private Scriptable mScriptScope;

    private Script mCurrentScript;
    private Script mPausedScript;
    private boolean mCurrentScriptDialog;
    private StringBuilder mScriptBuilder = new StringBuilder();
    private HashMap<Integer,Runnable> mScriptTimeouts = new HashMap<>();

    private ContinuationPending mPickImageContinuation;
    private int mPickImageMaxPixels;

    private ContinuationPending mCropImageContinuation;

    // TODO remove this hack, was needed to get a context while throwing execution in callbacks
    private static ScriptExecutor sCurrentScriptExecutor;
    public static ScriptExecutor getCurrent() {
        return sCurrentScriptExecutor;
    }

    public ScriptExecutor(LightningEngine engine) {
        mEngine = engine;
        mHandler = new Handler();

        mScriptLightning = new Lightning(engine);
        mScriptLL = new LL(mScriptLightning);
        mScriptAndroid = new Android(engine.getContext());
    }

    public LightningEngine getEngine() {
        return mEngine;
    }

    public Scriptable prepareScriptScope() {
        if(mScriptScope == null) {
            org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
            Scriptable root_scope = cx.initStandardObjects();
            cx.setOptimizationLevel(-1);

            Scriptable lightning = cx.getWrapFactory().wrapNewObject(cx, root_scope, mScriptLightning);
            mScriptScope = cx.newObject(lightning);
            mScriptScope.setPrototype(lightning);
            Object ll = org.mozilla.javascript.Context.javaToJS(mScriptLL, mScriptScope);
            ScriptableObject.putProperty(mScriptScope, "LL", ll);
            Object android = org.mozilla.javascript.Context.javaToJS(mScriptAndroid, mScriptScope);
            ScriptableObject.putProperty(mScriptScope, "Android", android);
            ScriptableObject.putProperty(mScriptScope, "self", mScriptScope);

            ScriptableObject.defineProperty(mScriptScope, "animate", new NativeFunction() {
                private String[] mParamOrVarNames = new String[]{"var", "duration", "type"};

                @Override
                protected int getLanguageVersion() {
                    return org.mozilla.javascript.Context.VERSION_1_4;
                }

                @Override
                protected int getParamCount() {
                    return 3;
                }

                @Override
                protected int getParamAndVarCount() {
                    return 3;
                }

                @Override
                protected String getParamOrVarName(int index) {
                    return mParamOrVarNames[index];
                }

                @Override
                public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    int offset = 0;
                    if (args.length >= 4) {
                        Object o = args[3];
                        offset = Value.asInteger(o);
                        if (offset < 0) {
                            throw new IllegalArgumentException("Offset must be a numeric value > 0");
                        }
                    }

                    String interpolator = VariableManager.DEFAULT_INTERPOLATOR.first;
                    if (args.length >= 3) {
                        Object o = args[2];
                        if (o != null && o.getClass() == String.class) {
                            interpolator = (String) o;
                        } else {
                            throw new IllegalArgumentException("Interpolator type must be a string");
                        }
                    }

                    int duration = 400;
                    if (args.length >= 2) {
                        Object o = args[1];
                        duration = Value.asInteger(o);
                        if (duration <= 0) {
                            throw new IllegalArgumentException("Duration must be a numeric value >= 0");
                        }
                    }

                    String name;
                    if (args.length >= 1 && args[0] != null && args[0].getClass() == String.class) {
                        name = (String) args[0];
                    } else {
                        throw new IllegalArgumentException("Variable name is required");
                    }
                    return mEngine.getVariableManager().animateVariable(name, duration, interpolator, offset);
                }
            }, 0);

            ScriptableObject.defineProperty(mScriptScope, "Script", new NativeJavaClass(mScriptScope, net.pierrox.lightning_launcher.script.api.Script.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Palette", new NativeJavaClass(mScriptScope, net.pierrox.lightning_launcher.script.api.palette.Palette.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "EventHandler", new NativeJavaClass(mScriptScope, EventHandler.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Property", new NativeJavaClass(mScriptScope, Property.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Binding", new NativeJavaClass(mScriptScope, Binding.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "RectL", new NativeJavaClass(mScriptScope, RectL.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Image", new NativeJavaClass(mScriptScope, Image.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Menu", new NativeJavaClass(mScriptScope, Menu.class), 0);

            ScriptableObject.defineProperty(mScriptScope, "Color", new NativeJavaClass(mScriptScope, Color.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Paint", new NativeJavaClass(mScriptScope, Paint.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Shader", new NativeJavaClass(mScriptScope, Shader.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "LinearGradient", new NativeJavaClass(mScriptScope, LinearGradient.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "RadialGradient", new NativeJavaClass(mScriptScope, RadialGradient.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "SweepGradient", new NativeJavaClass(mScriptScope, SweepGradient.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Path", new NativeJavaClass(mScriptScope, Path.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Matrix", new NativeJavaClass(mScriptScope, Matrix.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "RectF", new NativeJavaClass(mScriptScope, RectF.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Region", new NativeJavaClass(mScriptScope, Region.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "PorterDuff", new NativeJavaClass(mScriptScope, PorterDuff.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "PorterDuffXfermode", new NativeJavaClass(mScriptScope, PorterDuffXfermode.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Typeface", new NativeJavaClass(mScriptScope, Typeface.class), 0);

            ScriptableObject.defineProperty(mScriptScope, "Context", new NativeJavaClass(mScriptScope, Context.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Intent", new NativeJavaClass(mScriptScope, Intent.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Uri", new NativeJavaClass(mScriptScope, Uri.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "ComponentName", new NativeJavaClass(mScriptScope, ComponentName.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Bundle", new NativeJavaClass(mScriptScope, Bundle.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Toast", new NativeJavaClass(mScriptScope, Toast.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "Log", new NativeJavaClass(mScriptScope, Log.class), 0);

            ScriptableObject.defineProperty(mScriptScope, "MotionEvent", new NativeJavaClass(mScriptScope, MotionEvent.class), 0);

            ScriptableObject.defineProperty(mScriptScope, "TaskerIntent", new NativeJavaClass(mScriptScope, TaskerIntent.class), 0);
            ScriptableObject.defineProperty(mScriptScope, "ActionCodes", new NativeJavaClass(mScriptScope, ActionCodes.class), 0);

            org.mozilla.javascript.Context.exit();
        }

        return mScriptScope;
    }

    public void terminate() {
        for(Runnable timeout : mScriptTimeouts.values()) {
            mHandler.removeCallbacks(timeout);
        }
        mScriptTimeouts.clear();
        mScriptLightning.terminate();
    }

    public Handler getHandler() {
        return mHandler;
    }

    public Script getCurrentScript() {
        return mCurrentScript;
    }

    public Scriptable getScriptScope() {
        return mScriptScope;
    }

    public boolean bindClass(String name) {
        try {
            Class<?> cls = Class.forName(name);
            String simpleName = cls.getSimpleName();
            if(mScriptScope.has(simpleName, null)) {
                return false;
            } else {
                ScriptableObject.defineProperty(mScriptScope, simpleName, new NativeJavaClass(mScriptScope, cls), 0);
                return true;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

    }

    public int setTimeout(final Object function, int delayMillis) {
        Runnable timeout = new Runnable() {
            @Override
            public void run() {
                mScriptTimeouts.remove(this.hashCode());
                org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
                try {
                    cx.callFunctionWithContinuations((Callable) function, mScriptScope, new Object[]{});
                } catch(ContinuationPending cp) {
                    // will happen when displaying a popup
                    mPausedScript = mCurrentScript;
                } catch(RhinoException e) {
                    displayScriptError(e);
                } finally {
                    // Exit from the context.
                    org.mozilla.javascript.Context.exit();
                }
            }
        };
        int id = timeout.hashCode();
        mScriptTimeouts.put(id, timeout);
        mHandler.postDelayed(timeout, delayMillis);
        return id;
    }

    public void clearTimeout(int id) {
        Runnable timeout = mScriptTimeouts.remove(id);
        if(timeout != null) {
            mHandler.removeCallbacks(timeout);
        }
    }

    public void displayScriptError(RhinoException e) {
        displayScriptError("At line "+e.lineNumber()+": "+e.details(), e.lineNumber());
        e.printStackTrace();
    }

    public void displayScriptError(final String message, final int line) {
        final Context context = mScriptLightning.getScriptScreen().getContext();
        final int script_id = mCurrentScript.id;

        if(mCurrentScript.hasFlag(Script.FLAG_DISABLED) || mCurrentScriptDialog) {
            return;
        } else if(context instanceof Activity) {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(mCurrentScript.name);
            builder.setMessage(message);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mCurrentScriptDialog = false;
                }
            });
            final LLApp app = LLApp.get();
            if(app.hasScriptEditor()) {
                builder.setNeutralButton(R.string.sc_view, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mCurrentScriptDialog = false;
                        app.startScriptEditor(script_id, line);
                    }
                });
            }
            builder.setNegativeButton(R.string.sc_disable, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mCurrentScriptDialog = false;
                    mCurrentScript.setFlag(Script.FLAG_DISABLED, true);
                }
            });
            builder.setCancelable(false);
            mCurrentScriptDialog = true;
            builder.create().show();
        } else {
            final String name = mCurrentScript.name;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MultiPurposeTransparentActivity.startForScriptError(context, name, message, script_id, line);
                }
            });

        }
    }

    public boolean displayScriptDialog(String message, final String input, boolean has_cancel, final ContinuationPending pending) {
        final Context context = mScriptLightning.getScriptScreen().getContext();
        if(mCurrentScript.hasFlag(Script.FLAG_DISABLED) || mCurrentScriptDialog || !(context instanceof Activity)) {
            // need to execute this later so that the caller can finish its execution, and throw the ContinuationPending exception
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    continuePendingContinuation(pending, input == null ? false : input);
                }
            });
        } else {
            final Script script = mCurrentScript;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(mCurrentScript.name);
            View content = LayoutInflater.from(context).inflate(R.layout.dialog_script, null);
            ((TextView)content.findViewById(R.id.msg)).setText(message);
            final EditText input_text = (EditText) content.findViewById(R.id.input);
            input_text.setVisibility(input==null ? View.GONE : View.VISIBLE);

            if(input != null) {
                input_text.setText(input);
                input_text.setSelection(0, input.length());
            }
            builder.setView(content);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mCurrentScriptDialog = false;
                    continuePendingContinuation(pending, input == null ? true : input_text.getText().toString());
                }
            });
            if(has_cancel) {
                builder.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mCurrentScriptDialog = false;
                        continuePendingContinuation(pending, input == null ? false : null);
                    }
                });
            }
            builder.setNegativeButton(R.string.sc_disable, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mCurrentScriptDialog = false;
                    script.setFlag(Script.FLAG_DISABLED, true);
                    continuePendingContinuation(pending, input == null ? false : null);
                }
            });
            builder.setCancelable(false);
            mCurrentScriptDialog = true;
            builder.create().show();
        }
        return true;
    }

    public void displayScriptPickImageDialog(Screen screen, int maxPixels, final ContinuationPending pending) {
        mPickImageContinuation = pending;
        mPickImageMaxPixels = maxPixels;
        screen.displayScriptPickImageDialog(this);
    }

    public void displayScriptCropImageDialog(Screen screen, ImageBitmap image, boolean full_size, final ContinuationPending pending) {
        mCropImageContinuation = pending;
        screen.displayCropPickImageDialog(this, image, full_size);
    }

    public void displayScriptPickColorDialog(Screen screen, String title, int color, boolean hasAlpha, final ContinuationPending pending) {
        ColorPickerDialog color_picker_dialog = new ColorPickerDialog(screen.getContext(), color);
        color_picker_dialog.setAlphaSliderVisible(hasAlpha);
        color_picker_dialog.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                continuePendingContinuation(pending, Integer.valueOf(color));
            }

            @Override
            public void onColorDialogSelected(int color) {

            }

            @Override
            public void onColorDialogCanceled() {
                continuePendingContinuation(pending, null);
            }
        });
        color_picker_dialog.setTitle(title);
        color_picker_dialog.show();
    }

    public void displayScriptPickValueDialog(Screen screen, String title, float value, boolean is_float, float min, float max, float interval, String unit, final ContinuationPending pending) {
        DialogPreferenceSlider d = new DialogPreferenceSlider(screen.getContext(), value, is_float, min, max, interval, unit, new DialogPreferenceSlider.OnDialogPreferenceSliderListener() {
            @Override
            public void onDialogPreferenceSliderValueSet(float value) {
                continuePendingContinuation(pending, Float.valueOf(value));
            }

            @Override
            public void onDialogPreferenceSliderCancel() {
                continuePendingContinuation(pending, null);
            }
        });
        d.setTitle(title);

        d.show();
    }

    public void setFileForPickImage(File image_file) {
        if(image_file == null) {
            continuePendingContinuation(mPickImageContinuation, null);
            mPickImageContinuation = null;
        } else {
            Image image = null;
            if(Utils.isGifFile(image_file)) {
                AnimationDecoder decoder = new AnimationDecoder();
                if(decoder.read(image_file)) {
                    SharedAsyncGraphicsDrawable sd = new SharedAsyncGraphicsDrawable(new Graphics(decoder, decoder.getWidth(), decoder.getHeight()), false);
                    image = new ImageAnimation(mScriptLightning, sd);
                }
            } else if(Utils.isSvgFile(image_file)) {
                SvgDrawable svgDrawable = new SvgDrawable(image_file);
//                SharedAsyncGraphicsDrawable sd = new SharedAsyncGraphicsDrawable(new Graphics(svgDrawable), false);
                image = new ImageSvg(mScriptLightning, svgDrawable);
            }
            if(image == null){
                Bitmap bitmap = Utils.loadBitmap(image_file, mPickImageMaxPixels, 0, 0);
                if (bitmap != null) {
                    byte[] chunk = bitmap.getNinePatchChunk();
                    if (NinePatch.isNinePatchChunk(chunk)) {
                        NinePatch np = new NinePatch(bitmap, chunk, null);
                        image = new ImageNinePatch(mScriptLightning, np);
                    } else {
                        image = new ImageBitmap(mScriptLightning, bitmap);
                    }
                } else {
                    image = null;
                }
            }
            continuePendingContinuation(mPickImageContinuation, image);
            mPickImageContinuation = null;
        }
    }

    public void setImageForCropImage(ImageBitmap image) {
        if(image == null) {
            continuePendingContinuation(mCropImageContinuation, null);
            mCropImageContinuation = null;
        } else {
            continuePendingContinuation(mCropImageContinuation, image);
            mCropImageContinuation = null;
        }
    }

    public void continuePendingContinuation(ContinuationPending pending, Object result) {
        org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
        try {
            try {
                mCurrentScript = mPausedScript;
                cx.resumeContinuation(pending.getContinuation(), mScriptScope, result);
            } catch(ContinuationPending e) {
                mPausedScript = mCurrentScript;
            } catch(RhinoException e) {
                displayScriptError(e);
            }
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    org.mozilla.javascript.Context mCurrentContext;

    public void throwError(String message) {
        if(mCurrentContext != null) {
            throw ScriptRuntime.throwError(mCurrentContext, mScriptScope, message);
        }
    }

    private Script mRecursionScript;
    private ItemView mRecursionItemView;


    public void runScript(Screen screen, int id, String source, String data) {
        Script script = mEngine.getScriptManager().getOrLoadScript(id);
        runScript(screen, script, source, data, null, null);
    }

    public void runScript(Screen screen, String name, String source, String data) {
        runScript(screen, null, name, source, data);
    }

    public void runScript(Screen screen, String path, String name, String source, String data) {
        Script script = mEngine.getScriptManager().getOrLoadScript(path, name);
        runScript(screen, script, source, data, null, null);
    }

    public void runScript(Screen screen, int id, String source, String data, ItemLayout il) {
        Script script = mEngine.getScriptManager().getOrLoadScript(id);
        runScript(screen, script, source, data, il, null);
    }

    public static final String PROPERTY_EVENT_SCREEN = "ev_sc";
    public void runScript(Screen screen, Script script, String source, String data, ItemLayout il, ItemView itemView) {
        if(!canRunScript(script)) {
            return;
        }

        mCurrentScript = script;
        if(mCurrentScript != null && !mCurrentScript.hasFlag(Script.FLAG_DISABLED)) {
            if(script == mRecursionScript && ((itemView ==null && mRecursionItemView ==null) || (itemView !=null && itemView.equals(mRecursionItemView)))) {
//                String details = "Recursion detected";
//                if(item != null) {
//                    details += ", item: "+item.formatForDisplay(true, 0);
//                }
//                if(source != null) {
//                    details += ", source: "+source;
//                }
//                displayScriptError(new EvaluatorException(details, script.name, 0));
                mRecursionScript = script;
                mRecursionItemView = itemView;
                return;
            }

            mRecursionScript = script;
            mRecursionItemView = itemView;

            prepareScriptScope();

//            LLApp.get().onScriptRun();

            org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
            mCurrentContext = cx;

            sCurrentScriptExecutor = this;

            ScriptableObject.putProperty(mScriptScope, PROPERTY_EVENT_SCREEN, screen);
            ScriptableObject.putProperty(mScriptScope, "ev_se", source);
            ScriptableObject.putProperty(mScriptScope, "ev_d", data);
            ScriptableObject.putProperty(mScriptScope, "ev_t", System.currentTimeMillis());
            ScriptableObject.putProperty(mScriptScope, "ev_il", il);
            ScriptableObject.putProperty(mScriptScope, "ev_iv", itemView);

            try {
                if(mCurrentScript.compiledScript == null) {
                    mScriptBuilder.setLength(0);
                    mScriptBuilder.append("javascript:(function() {var _event = createEvent(ev_sc, ev_se, ev_d, ev_t, ev_il, ev_iv); var getEvent = function() { return _event;};\n")
                            .append(mCurrentScript.getScriptText())
                            .append("\n})();");
                    cx.setOptimizationLevel(-1);
                    mCurrentScript.compiledScript = cx.compileString(mScriptBuilder.toString(), script.name, 0, null);
                }

                if(mCurrentScript.compiledScript != null) {
                    try {
                        cx.executeScriptWithContinuations(mCurrentScript.compiledScript, mScriptScope);
                    } catch(ContinuationPending cp) {
                        // will happen when displaying a popup
                        mPausedScript = mCurrentScript;
                    } catch(IllegalStateException e) {
                        // running a script from an already running script with continuation
                        // try to run it again without support for continuation
                        mCurrentScript.compiledScript.exec(cx, mScriptScope);
                    }
                }
            } catch(RhinoException e) {
                displayScriptError(e);
                e.printStackTrace();
            } finally {
                // Exit from the context.
                org.mozilla.javascript.Context.exit();
                mCurrentContext = null;
            }

            mRecursionScript = null;
            mRecursionItemView = null;
        }
    }



    public boolean runScriptTouchEvent(Screen screen, int id, ItemView itemView, MotionEvent event) {
        Object res = runScriptAsFunction(screen, id, "item, event", new Object[] { mScriptLightning.getCachedItem(itemView), event }, true, true);
        if(res == null || res.getClass()!=Boolean.class) {
            return false;
        } else {
            return (Boolean)res;
        }
    }

    public Object runScriptMenu(Screen screen, int id, ItemLayout il, ItemView itemView, Menu menu, String data) {
        return runScriptAsFunction(screen, id, "container, item, menu, data",
                new Object[] {
                    il == null ? null : mScriptLightning.getCachedContainer(il),
                    itemView == null ? null : mScriptLightning.getCachedItem(itemView),
                    menu, data
            },
                true, true);
    }

    public Object runScriptFunction(Screen screen, int id, ItemView itemView, String data) {
        return runScriptAsFunction(screen, id, "item, data", new Object[] { mScriptLightning.getCachedItem(itemView), data }, false, true);
    }

    public void runScriptActivityResult(Screen screen, int resultCode, Intent data, int id, String token) {
        runScriptAsFunction(screen, id, "resultCode, data, token", new Object[] { resultCode, data, token}, true, true);
    }

    public Object runScriptAsFunction(Screen screen, int id, String parameters, Object[] arguments, boolean allow_continuation, boolean display_errors) {
        Script script = mEngine.getScriptManager().getOrLoadScript(id);
        if(!canRunScript(script)) {
            return null;
        }

        mCurrentScript = script;
        if(mCurrentScript != null && !mCurrentScript.hasFlag(Script.FLAG_DISABLED)) {
            prepareScriptScope();

//            LLApp.get().onScriptRun();

            org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
            mCurrentContext = cx;

            ScriptableObject.putProperty(mScriptScope, PROPERTY_EVENT_SCREEN, screen);

            try {
                if(mCurrentScript.compiledFunction == null) {
                    mScriptBuilder.setLength(0);
                    mScriptBuilder.append("function(")
                            .append(parameters)
                            .append(") {\n")
                            .append(mCurrentScript.getScriptText())
                            .append("\n}");

                    cx.setOptimizationLevel(-1);
                    mCurrentScript.compiledFunction = cx.compileFunction(mScriptScope, mScriptBuilder.toString(), script.name, 0, null);
                }

                return runFunction(mCurrentScript.compiledFunction, arguments, allow_continuation, display_errors);
            } catch(RhinoException e) {
                if(display_errors) {
                    displayScriptError(e);
                }
            } finally {
                // Exit from the context.
                org.mozilla.javascript.Context.exit();
                mCurrentContext = null;
            }
        }

        return null;
    }

    public Object runFunction(Function function, Object[]arguments, boolean allow_continuation, boolean display_errors) {
        if(!canRunScriptGlobally()) {
            return null;
        }

        prepareScriptScope();

        org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
        mCurrentContext = cx;
        sCurrentScriptExecutor = this;
        try {
            if(allow_continuation) {
                try {
                    return cx.callFunctionWithContinuations(function, mScriptScope, arguments);
                } catch (IllegalStateException e) {
                    return function.call(cx, mScriptScope, mScriptScope, arguments);
                }
            } else {
                return function.call(cx, mScriptScope, mScriptScope, arguments);
            }
        } catch(ContinuationPending cp) {
            // will happen when displaying a popup
            mPausedScript = mCurrentScript;
        } catch(RhinoException e) {
            if(display_errors) {
                displayScriptError(e);
            }
        } catch(Throwable t) {
            if(display_errors) {
                displayScriptError(t.getMessage(), -1);
            }
            t.printStackTrace();
        } finally {
            // Exit from the context.
            org.mozilla.javascript.Context.exit();
            mCurrentContext = null;
        }

        return null;
    }

    private boolean canRunScriptGlobally() {
        return mEngine.canRunScripts() && !LLApp.get().isFreeVersion();
    }

    private boolean canRunScript(Script script) {
        if(script == null) {
            return false;
        }

        if(script.id < Script.NO_ID) {
            return true;
        }

        return canRunScriptGlobally();
    }

    public Lightning getLightning() {
        return mScriptLightning;
    }
}
