package net.pierrox.lightning_launcher.script.api;

import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.script.ScriptExecutor;
import net.pierrox.lightning_launcher.script.api.screen.Screen;

import java.util.HashMap;

/**
 * This object is used to access script properties such as text, tag and menu attributes.
 * Warning: this API is currently experimental.
 */
public class Script {
    public static final int FLAG_ALL = net.pierrox.lightning_launcher.script.Script.FLAG_ALL;
    public static final int FLAG_DISABLED = net.pierrox.lightning_launcher.script.Script.FLAG_DISABLED;
    public static final int FLAG_APP_MENU = net.pierrox.lightning_launcher.script.Script.FLAG_APP_MENU;
    public static final int FLAG_ITEM_MENU = net.pierrox.lightning_launcher.script.Script.FLAG_ITEM_MENU;
    public static final int FLAG_CUSTOM_MENU = net.pierrox.lightning_launcher.script.Script.FLAG_CUSTOM_MENU;

    private LightningEngine mEngine;
    private net.pierrox.lightning_launcher.script.Script mScript;

    /**
     * @hide
     */
    /*package*/ Script(LightningEngine engine, net.pierrox.lightning_launcher.script.Script script) {
        mEngine = engine;
        mScript = script;
    }

    /**
     * Run this script
     * @param screen screen in which to execute the script
     * @param data optional data to send to the script. Use JSON to pass more than a string.
     */
    public void run(final Screen screen, final String data) {
        final ScriptExecutor scriptExecutor = mEngine.getScriptExecutor();
        scriptExecutor.getHandler().post(new Runnable() {
            @Override
            public void run() {
                scriptExecutor.runScript(screen.getScreen(), mScript.id, "RUN_SCRIPT", data);
            }
        });
    }

    /**
     * Retrieve the script's unique identifier.
     */
    public int getId() {
        return mScript.id;
    }

    /**
     * Retrieve the script's name.
     */
    public String getName() {
        return mScript.name;
    }

    /**
     * Change the script's name
     * @param name new name
     */
    public void setName(String name) {
        mScript.name = name;
        save();
    }

    /**
     * Set the path of this script. This is useful to group scripts by owner and/or category.
     * @param path use "/" as separator
     */
    public void setPath(String path) {
        mScript.setRelativePath(path);
    }

    /**
     * Retrieve the path of the script. Default is "/".
     */
    public String getPath() {
        return mScript.getRelativePath();
    }

    /**
     * Retrieve the script's text.
     * <font color="red">Experimental</font>: it is possible to structure a script library using <code>eval(some_script.getText())</code>, at the expense of execution speed. Also the "evaled" script cannot include any direct suspension (alert or similar).
     * Use a function to encapsulate code:
     * <code><pre>
     *     function doSomething() {
     *         alert("hello");
     *         ...
     *     }
     * </pre></code>
     * And then use it this way:
     * <code><pre>
     *     eval(getScriptByName("some_script").getText());
     *     doSomething();
     * </pre></code>
     */
    public String getText() {
        return mScript.getSourceText();
    }

    /**
     * Have fun with this, but not too much !!!
     */
    public void setText(String text) {
        mScript.setSourceText(text);
        save();
    }

    /**
     * Check for the presence of a given flag
     * @param flag one of FLAG_*
     * @return true if the flag is set
     */
    public boolean hasFlag(int flag) {
        return mScript.hasFlag(flag);
    }

    /**
     * Set a flag
     * @param flag one of FLAG_*
     * @param on whether to set or clear the flag
     */
    public void setFlag(int flag, boolean on) {
        mScript.setFlag(flag, on);
        save();
    }

    /**
     * Set a persistent tag data for the currently executing script. This value will be saved and can be retrieved later using {@link #getTag()}, including after an application restart.
     * @param tag data to store. Use JSON to pass more than a string.
     */
    public void setTag(String tag) {
        mScript.tag = tag;
        save();
    }

    /**
     * Retrieve the data associated to the currently executing script.
     */
    public String getTag() {
        return mScript.tag;
    }

    /**
     * Set a custom and persistent data for this container.
     * Using a null id is the same as using {@link #setTag(String)} without the id argument.
     * @see Item#setTag(String, String)
     */
    public void setTag(String id, String value) {
        if(id == null) {
            setTag(value);
        } else {
            if (value == null) {
                if (mScript.tags != null) {
                    mScript.tags.remove(id);
                }
            } else {
                if (mScript.tags == null) {
                    mScript.tags = new HashMap<String, String>(1);
                }
                mScript.tags.put(id, value);
            }
            save();
        }
    }

    /**
     * Returns the value associated with this container. Can be undefined if it has never been set.
     * Using a null id is the same as using {@link #getTag()} without argument.
     * @see Item#getTag(String)
     */
    public String getTag(String id) {
        if(id == null) {
            return getTag();
        } else {
            return mScript.tags == null ? null : mScript.tags.get(id);
        }
    }

    private void save() {
        mEngine.getScriptManager().saveScript(mScript);
    }

    /**
     * @hide
     */
    /*package*/ net.pierrox.lightning_launcher.script.Script getScript() {
        return mScript;
    }
}
