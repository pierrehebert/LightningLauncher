package net.pierrox.lightning_launcher.script.api;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.SparseArray;

import com.faendir.rhino_android.RhinoAndroidHelper;

import net.dinglisch.android.tasker.TaskerIntent;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.api.ScreenIdentity;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.script.ScriptManager;
import net.pierrox.lightning_launcher.engine.variable.VariableManager;
import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.api.screen.ActivityScreen;
import net.pierrox.lightning_launcher.script.api.screen.HomeScreen;
import net.pierrox.lightning_launcher.script.api.screen.Screen;
import net.pierrox.lightning_launcher.views.ItemLayout;
import net.pierrox.lightning_launcher.views.item.ItemView;

import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Interpreter;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UniqueTag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Entry point for most Lightning Launcher scripted features.
 * The Lightning object is the root object in the script context, hence its functions can be called without naming it, unlike with the deprecated LL object.
 * For instance, instead of using <code>LL.getDesktopByName('d')</code>, simply use <code>getDesktopByName('d')</code>
 */
public class Lightning {

    private LightningEngine mEngine;
    private SparseArray<Container> mCachedContainers = new SparseArray<>();
    private SparseArray<Item> mCachedItems = new SparseArray<>();

    /**
     * @hide
     */
    public Lightning(LightningEngine engine) {
        mEngine = engine;


        mEngine.registerPageListener(mPageListener);
    }

    /**
     * @hide
     */
    public void terminate() {
        mEngine.unregisterPageListener(mPageListener);
        mCachedContainers.clear();
        mCachedItems.clear();
    }

    /**
     * @hide
     */
    private Page.EmptyPageListener mPageListener = new Page.EmptyPageListener() {
        @Override
        public void onPageRemoved(Page page) {
            int i;

            for(i=mCachedContainers.size()-1; i>=0; i--) {
                Container container = mCachedContainers.valueAt(i);
                if (container.getPage() == page) {
                    mCachedContainers.remove(mCachedContainers.keyAt(i));
                }
            }

            for (i=mCachedItems.size()-1; i>=0; i--) {
                Item item = mCachedItems.valueAt(i);
                if(item.getItem().getPage() == page) {
                    mCachedItems.remove(mCachedItems.keyAt(i));
                }
            }
        }

        @Override
        public void onPageItemRemoved(Page page, net.pierrox.lightning_launcher.data.Item item) {
            for (int i=mCachedItems.size()-1; i>=0; i--) {
                Item scriptItem = mCachedItems.valueAt(i);
                if(scriptItem.getItem() == item) {
                    mCachedItems.remove(mCachedItems.keyAt(i));
                }
            }
        }
    };

    /**
     * @hide
     */
    public LightningEngine getEngine() {
        return mEngine;
    }

    /**
     * @hide
     */
	/*package*/ void scriptError(String message) {
        mEngine.getScriptExecutor().throwError(message);
    }


    /**
     * @hide
     */
    public Container getCachedContainer(ItemLayout il) {
        if (il == null) {
            return null;
        }
        int id = il.hashCode();
        Container container = mCachedContainers.get(id);
        if (container == null) {
            if (il.getPage().isDashboard()) {
                container = new Desktop(this, il);
            } else {
                container = new Container(this, il);
            }
            mCachedContainers.put(id, container);
        }

        return container;
    }

    /**
     * @hide
     */
    public void clearCachedContainer(ItemLayout il) {
        mCachedContainers.remove(il.hashCode());
    }

    /**
     * Returns a script item object, creates it if needed. This script item encapsulates an item view (and the linked item data).
     * @hide
     */
    public Item getCachedItem(ItemView itemView) {
        int id = itemView.hashCode();
        Item i = mCachedItems.get(id);
        if (i == null) {
            net.pierrox.lightning_launcher.data.Item baseItem = itemView.getItem();
            Class<?> cls = baseItem.getClass();
            if (cls == net.pierrox.lightning_launcher.data.Shortcut.class) {
                i = new Shortcut(this, itemView);
            } else if (cls == net.pierrox.lightning_launcher.data.Folder.class) {
                i = new Folder(this, itemView);
            } else if (cls == net.pierrox.lightning_launcher.data.EmbeddedFolder.class) {
                i = new Panel(this, itemView);
            } else if (cls == net.pierrox.lightning_launcher.data.StopPoint.class) {
                i = new StopPoint(this, itemView);
            } else if (cls == net.pierrox.lightning_launcher.data.PageIndicator.class) {
                i = new PageIndicator(this, itemView);
            } else if (cls == net.pierrox.lightning_launcher.data.CustomView.class) {
                i = new CustomView(this, itemView);
            } else {
                i = new Item(this, itemView);
            }
            mCachedItems.put(id, i);
        }
        return i;
    }

    /**
     * Returns a script item object, only if it has already been cached.
     * @hide
     */
    public Item findCachedItem(ItemView itemView) {
        int id = itemView.hashCode();
        return mCachedItems.get(id);
    }

    /**
     * @hide
     */
    public void updateCachedItem(Item item, ItemView newItemView) {
        mCachedItems.remove(item.mItemView.hashCode());
        item.mItemView = newItemView;
        ItemLayout il = newItemView.getParentItemLayout();
        if(il != null) {
            il.ensureItemViewReady(newItemView);
        }
        mCachedItems.put(newItemView.hashCode(), item);
    }

    /**
     * Returns a script container object, only if it has already been cached.
     * @hide
     */
    public Container findCachedContainer(ItemLayout itemLayout) {
        int id = itemLayout.hashCode();
        return mCachedContainers.get(id);
    }

    /**
     * @hide
     */
    public void updateCachedContainer(Container container, ItemLayout newItemLayout) {
        mCachedContainers.remove(container.mItemLayout.hashCode());
        container.mItemLayout = newItemLayout;
        mCachedContainers.put(newItemLayout.hashCode(), container);
    }

    /**
     * @hide
     */
    public void clearCachedItem(ItemView itemView) {
        mCachedItems.remove(itemView.hashCode());
    }

    /**
     * Returns the event object associated with this script execution.
     * The event provides contextual data, such as the current desktop, which item has been tapped, and so on.
     */
    public Event getEvent_() {
        return null;
    }

    /**
     * @hide
     */
    public Event createEvent(net.pierrox.lightning_launcher.engine.Screen screen, String source, String data, long date, ItemLayout il, ItemView iv) {
        return new Event(this, createScreen(screen), source, data, date, il, iv);
    }

    /**
     * @hide
     */
    /*package*/ Event findEventInStack() {
        // XXX HACK find the local event (closure) in the call stack
        org.mozilla.javascript.Context context = org.mozilla.javascript.Context.getCurrentContext();
        if(context == null) {
            return null;
        }

        Interpreter.CallFrame lastInterpreterFrame = (Interpreter.CallFrame) context.lastInterpreterFrame;

        if(lastInterpreterFrame == null) {
            return null;
        }

        Scriptable scope = lastInterpreterFrame.scope;
        for(;;) {
            Object e = scope.get("_event", scope);
            if(e != UniqueTag.NOT_FOUND) {
                return (Event) ((NativeJavaObject)e).unwrap();
            }

            scope = scope.getParentScope();
            if(scope == null) {
                return null;
            }
        }
    }

    /**
     * @hide
     */
    public Screen getScriptScreen() {
        Event event = findEventInStack();
        if(event == null) {
            Scriptable scope = mEngine.getScriptExecutor().getScriptScope();
            return createScreen((net.pierrox.lightning_launcher.engine.Screen) scope.get(ScriptExecutor.PROPERTY_EVENT_SCREEN, scope));
        } else {
            return event.getScreen();
        }
    }
    /**
     * Retrieve the configuration object used to get and set launcher general settings.
     */
    public Configuration getConfiguration() {
        return new Configuration(this);
    }

    /**
     * Retrieve the currently active screen, the one which is displayed to the user.
     * Note that this is often the same as Lightning.getEvent().getScreen(), but not always : events and script execution can occur in non active screens.
     * @return the active screen, or null if no screen is active at the moment
     */
    public Screen getActiveScreen() {
        return createScreen(LLApp.get().getActiveScreen());
    }

    /**
     * Return the home screen, null if not created yet
     */
    public HomeScreen getHomeScreen() {
        return (HomeScreen) createScreen(LLApp.get().getScreen(net.pierrox.lightning_launcher.api.ScreenIdentity.HOME));
    }

    /**
     * Return the app drawer screen, null if not created yet
     */
    public ActivityScreen getAppDrawerScreen() {
        return (ActivityScreen) createScreen(LLApp.get().getScreen(net.pierrox.lightning_launcher.api.ScreenIdentity.APP_DRAWER));
    }

    /**
     * Return the lock screen, null if not created yet
     */
    public ActivityScreen getLockScreen() {
        return (ActivityScreen) createScreen(LLApp.get().getScreen(net.pierrox.lightning_launcher.api.ScreenIdentity.LOCK));
    }

    /**
     * Return the floating screen, null if not created yet
     */
    public Screen getFloatingScreen() {
        return createScreen(LLApp.get().getScreen(net.pierrox.lightning_launcher.api.ScreenIdentity.FLOATING));
    }

    /**
     * Return the live wallpaper screen, null if not created yet
     */
    public Screen getLiveWallpaperScreen() {
        return createScreen(LLApp.get().getScreen(net.pierrox.lightning_launcher.api.ScreenIdentity.LIVE_WALLPAPER));
    }

    /**
     * Return the background screen. This special screen is used when running background scripts. It can be used to query items and containers, but position and size may not be computed since the screen is not displayed and has no size.
     */
    public Screen getBackgroundScreen() {
        return createScreen(LLApp.get().getScreen(net.pierrox.lightning_launcher.api.ScreenIdentity.BACKGROUND));
    }

    public Screen createScreen(net.pierrox.lightning_launcher.engine.Screen screen) {
        if(screen == null){
            return null;
        } else if(screen.getIdentity() == ScreenIdentity.HOME) {
            return new HomeScreen(this, screen);
        } else if(screen.getContext() instanceof Activity) {
            return new ActivityScreen(this, screen);
        } else {
            return new Screen(this, screen);
        }
    }

    /**
     * Set a boolean variable. This is a shortcut for <code>getVariables().edit().setBoolean(name, value).commit();</code>. When modifying several at once, consider using the {@link net.pierrox.lightning_launcher.script.api.PropertyEditor} object instead for best efficiency.
     */
    public void setVariableBoolean(String name, boolean value) {
        setVariable(name, value);
    }

    /**
     * Set a boolean variable. This is a shortcut for <code>getVariables().edit().setInteger(name, value).commit();</code>. When modifying several at once, consider using the {@link net.pierrox.lightning_launcher.script.api.PropertyEditor} object instead for best efficiency.
     */
    public void setVariableInteger(String name, long value) {
        setVariable(name, (int)value);
    }

    /**
     * Set a boolean variable. This is a shortcut for <code>getVariables().edit().setFloat(name, value).commit();</code>. When modifying several at once, consider using the {@link net.pierrox.lightning_launcher.script.api.PropertyEditor} object instead for best efficiency.
     */
    public void setVariableFloat(String name, float value) {
        if(Float.isNaN(value)) {
            throw ScriptRuntime.constructError("setFloat", "Bad argument");
        }
        setVariable(name, value);
    }

    /**
     * Set a string variable. This is a shortcut for <code>getVariables().edit().setString(name, value).commit();</code>. When modifying several at once, consider using the {@link net.pierrox.lightning_launcher.script.api.PropertyEditor} object instead for best efficiency.
     */
    public void setVariableString(String name, String value) {
        setVariable(name, value);
    }

    private void setVariable(String name, Object value) {
        VariableManager vm = mEngine.getVariableManager();
        vm.edit();
        vm.setVariable(name, value);
        vm.commit();
    }

    /**
     * Retrieve the whole set of known variables (builtins and user ones).
     */
    public VariableSet getVariables() {
        return new VariableSet(mEngine.getVariableManager());
    }

    /**
     * Retrieve the currently executed script.
     */
    public Script getCurrentScript() {
        return new Script(mEngine, mEngine.getScriptExecutor().getCurrentScript());
    }

    /**
     * Retrieve a script by name.
     * @param name as given by Script#getName()
     * @return a script or null if not found
     */
    public Script getScriptByName(String name) {
        return getScriptByPathAndName(null, name);
    }

    /**
     * Retrieve a script by its path and name.
     * @param path as given by Script#getPath()
     * @param name as given by Script#getName()
     * @return a script or null if not found
     */
    public Script getScriptByPathAndName(String path, String name) {
        net.pierrox.lightning_launcher.script.Script script = mEngine.getScriptManager().getOrLoadScript(path, name);
        return script == null ? null : new Script(mEngine, script);
    }

    /**
     * Retrieve a script by its unique identifier
     * @param id identifier as given by Script#getId()
     * @return a script or null if no script with this id
     */
    public Script getScriptById(String id) {
        try {
            int id_ = Integer.parseInt(id);
            return new Script(mEngine, mEngine.getScriptManager().getOrLoadScript(id_));
        } catch(NumberFormatException e) {
            return null;
        }
    }

    /**
     * Delete a script.
     */
    public void deleteScript(Script script) {
        mEngine.getScriptManager().deleteScript(script.getScript());
    }

    /**
     * Create a new script using the default path "/"
     * @deprecated use {@link #createScript(String, String, String, int)} instead
     */
    public Script createScript(String name, String text, int flags) {
        return createScript("/", name, text, flags);
    }

    /**
     * Create a new script. Use this API wisely.
     * if path is null, "/" is used.
     */
    public Script createScript(String path, String name, String text, int flags) {
        path = ScriptManager.sanitizeRelativePath(path);
        ScriptManager sm = mEngine.getScriptManager();
        net.pierrox.lightning_launcher.script.Script script = sm.createScriptForFile(name, path);
        script.setSourceText(text);
        script.flags = flags;
        sm.saveScript(script);
        return new Script(mEngine, script);
    }

    /**
     * Return the collection of scripts matching some flags.
     * @param flags see Script#FLAG_*
     */
    public Script[] getAllScriptMatching(int flags) {
        ArrayList<net.pierrox.lightning_launcher.script.Script> all_scripts = mEngine.getScriptManager().getAllScriptMatching(flags);
        int length = all_scripts.size();
        Script[] array = new Script[length];
        for(int i=0; i<length; i++) {
            array[i] = new Script(mEngine, all_scripts.get(i));
        }
        return array;
    }

    /**
     * Unlock the screen.
     */
    public void unlock() {
        LLApp.get().unlockLockScreen(true);
    }

    /**
     * Returns true if the screen is currently locked using the Lightning lock screen.
     */
    public boolean isLocked() {
        return LLApp.get().isLockScreenLocked();
    }

    /**
     * Write data to a file. This is for logging and debug purpose only. The path is not configurable and is: <external storage>/LightningLauncher/script/log.txt.
     * Please note that this method won't add newlines automatically when appending data.
     * @param data text to write to the file
     * @param append whether to clear the file before to write data, or append data to the existing content
     */
    public void writeToLogFile(String data, boolean append) {
        FileWriter fw = null;
        try {
            FileUtils.LL_EXT_SCRIPT_DIR.mkdirs();
            fw = new FileWriter(new File(FileUtils.LL_EXT_SCRIPT_DIR, "log.txt"), append);
            fw.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fw != null) try { fw.close(); } catch(Exception e) {}
        }
    }

    /**
     * This method is used to load a text from a package raw resource.
     * Instances of use of this method are:
     * <ul>
     *     <li>load scripts and set them as event handler (useful in script plugins)</li>
     *     <li>load JSON data, such as theme colors, data, etc.</li>
     * </ul>
     * @param pkg package name from which to read resources
     * @param name name of the raw resource. It must not contain the extension of the raw file, this is the Android identifier.
     * @return a string or null if the resource cannot be found or read
     */
    public String loadRawResource(String pkg, String name) {
        try {
            android.content.Context remote_context = mEngine.getContext().createPackageContext(pkg, 0);
            Resources rsrc = remote_context.getResources();
            int id = rsrc.getIdentifier(name, "raw", pkg);
            if(id != 0) {
                InputStream is = rsrc.openRawResource(id);
                return FileUtils.readInputStreamContent(is);
            }
        } catch (Throwable e) {
        }

        return null;
    }

    /**
     * Persist launcher data now.
     */
    public void save() {
        LLApp.get().saveAllData();
    }

    /**
     * Send a tasker intent, optionally waiting for its completion to return.
     * @param intent an intent built with TaskerIntent (see http://tasker.dinglisch.net/invoketasks.html for samples)
     * @param synchronous when true, Lightning will wait for Tasker task completion before to return, otherwise it will return immediately
     * @return when synchronous is true returns true if the intent has been sent successfully and Tasker reports a success too, when synchronous is false this method always returns true..
     */
    public boolean sendTaskerIntent(TaskerIntent intent, boolean synchronous) {
        final Context context = mEngine.getContext();
        if(synchronous) {
            org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
            final ContinuationPending pending;

            try {
                pending = cx.captureContinuation();
                BroadcastReceiver br = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent recIntent) {
                        boolean success = recIntent.getBooleanExtra(TaskerIntent.EXTRA_SUCCESS_FLAG, false);

                        context.unregisterReceiver(this);

                        mEngine.getScriptExecutor().continuePendingContinuation(pending, Boolean.valueOf(success));
                    }
                };
                context.registerReceiver(br, intent.getCompletionFilter());
                context.sendBroadcast(intent);
                throw pending;
            } catch(IllegalStateException e) {
                android.widget.Toast.makeText(context, "cannot wait for Tasker result in this context, set 'synchronous' to false", android.widget.Toast.LENGTH_SHORT).show();
                return false;
            } finally {
                org.mozilla.javascript.Context.exit();
            }
        } else {
            context.sendBroadcast(intent);
            return true;
        }
    }

    /**
     * Translate a Java class into a JavaScript object.
     * This is a convenience method that avoid repeated use of fully qualified names while scripting Java.
     * @param name fully qualified class name
     * @return true if the operation succeeded, false if the class cannot be loaded or if already bound
     */
    public boolean bindClass(String name) {
        return mEngine.getScriptExecutor().bindClass(name);
    }

    /**
     * Display a message in a dialog box.
     * @param message text to display
     */
    public void alert(String message) {
        displayDialog(message, null, false);
    }

    /**
     * Display a message in a dialog box with Ok/Cancel buttons
     * @param message text to display
     * @return true if the dialog box has been confirmed with the Ok button
     */
    public boolean confirm(String message) {
        displayDialog(message, null, true);

        // will never been reached, except if not run with continuations
        return false;
    }

    /**
     * Display a message in a dialog box with an input text area.
     * @param message text to display
     * @param input initial value in the input text
     * @return the inputed text, null if the dialog box has been canceled
     */
    public String prompt(String message, String input) {
        displayDialog(message, input, true);

        // will never been reached, except if not run with continuations
        return input;
    }

    private boolean displayDialog(String message, String input, boolean has_cancel) {
        Context context = getScriptScreen().getContext();
        if(context instanceof Activity) {
            org.mozilla.javascript.Context cx = RhinoAndroidHelper.prepareContext();
            try {
                ContinuationPending pending = cx.captureContinuation();
                mEngine.getScriptExecutor().displayScriptDialog(message, input, has_cancel, pending);
                throw pending;
            } catch (IllegalStateException e) {
                // not called with continuation support
                android.widget.Toast.makeText(context, "cannot display \"" + message + "\" in this context", android.widget.Toast.LENGTH_SHORT).show();
                return false;
            } finally {
                org.mozilla.javascript.Context.exit();
            }
        } else {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Execute a function later.
     * @param function need to be a function
     * @param delayMillis anything below 0 will be handled as 0
     * @return timeout id, can be used with #clearTimeout to cancel it, will return 0 if function is not a function
     */
    public int setTimeout(Object function, int delayMillis) {
        if(function instanceof org.mozilla.javascript.Script) {
            if(delayMillis<0) delayMillis = 0;
            return mEngine.getScriptExecutor().setTimeout((org.mozilla.javascript.Script) function, delayMillis);
        }

        return 0;
    }

    /**
     * Clear a timeout previously set with #setTimeout
     * @param id identifier returned by #setTimeout
     */
    public void clearTimeout(int id) {
        mEngine.getScriptExecutor().clearTimeout(id);
    }

    /**
     * return the typeface used to draw icons.
     */
    public Typeface getIconsTypeface() {
        return LLApp.get().getIconsTypeface();
    }

    /**
     * Encode a color as an hexadecimal string, prefixed with '#'. Arguments are color component ranging from 0 to 1. For instance calling argb(1, 1, 0, 0) will return #ffff0000
     */
    public String argb(float a, float r, float g, float b) {
        String hex = Integer.toHexString(Color.argb(Math.round(a*255), Math.round(r*255), Math.round(g*255), Math.round(b*255)));
        while(hex.length()<8) {
            hex='0'+hex;
        }
        return "#"+hex;
    }
}
