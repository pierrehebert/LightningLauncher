package net.pierrox.lightning_launcher.script;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import net.pierrox.lightning_launcher.BuildConfig;
import net.pierrox.lightning_launcher.R;
import net.pierrox.lightning_launcher.Version;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.variable.Binding;
import net.pierrox.lightning_launcher.script.api.Property;
import net.pierrox.lightning_launcher.views.item.ItemView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ScriptManager {
    public static final int BUILTIN_USER_MENU = -2;
    public static final int BUILTIN_REPOSITORY_IMPORTER = -3;
    private final Script[] BUILTINS;
    private static final String SCRIPT_DIR = "scripts";

    private LightningEngine mLightningEngine;
    private SparseArray<File> mScriptFiles = new SparseArray<>();
    private SparseArray<Script> mScripts = new SparseArray<>();
    private int mNextTranscientId = -0xff;

    public ScriptManager(LightningEngine lightningEngine) {
        mLightningEngine = lightningEngine;

        BUILTINS = new Script[] {
                new Script(this, Script.TYPE_BUILTIN, BUILTIN_USER_MENU, null,
                        "var item = LL.getEvent().getItem();\n"+
                                "LL.runAction(EventHandler.CLOSE_TOPMOST_FOLDER);\n"+
                                "switch(item.getName()) {\n"+
                                "  case 'wallpaper': LL.runAction(EventHandler.SELECT_WALLPAPER); break;\n"+
                                "  case 'theme': " +
                                "      var intent=new Intent(Intent.ACTION_VIEW, Uri.parse('"+ Version.BROWSE_TEMPLATES_URI+"'));"+
                                "      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);\n"+
                                "      LL.startActivity(intent);\n"+
                                "      break;\n"+
                                "  case 'add_item': LL.runAction(EventHandler.ADD_ITEM); break;\n"+
                                "  case 'edit_layout': LL.runAction(EventHandler.EDIT_LAYOUT); break;\n"+
                                "  case 'settings': LL.runAction(EventHandler.CUSTOMIZE_LAUNCHER); break;\n"+
                                "}",
                        null
                ),
                new Script(this, Script.TYPE_BUILTIN, BUILTIN_REPOSITORY_IMPORTER, null,
                        "/*Script necessary for the repository importer to work correctly.*/\n" +
                                "\n" +
                                "eval(\"function toEval(){\\n\"+LL.loadRawResource(\"com.trianguloy.llscript.repository\",\"executor\")+\"\\n}\");\n" +
                                "toEval();﻿",
                        null
                )
        };
    }

    public void init() {
        collectAllScriptsPaths(getScriptsDir());
    }

    public Script getOrLoadScript(int id) {
        Script script = mScripts.get(id);
        if (script != null) {
            return script;
        }

        // should be 0 but a bug produced scripts with id -1
        if (id >= -1) {
            File file = getScriptFile(id);
            if(file == null) {
                return null;
            }
            JSONObject json = FileUtils.readJSONObjectFromFile(file);
            if (json == null) {
                return null;
            } else {
                script = new Script(this, Script.TYPE_FILE, id, null, null, file);
                script.loadFieldsFromJSONObject(json, null);
                if (json.has("text")) {
                    try {
                        script.setSourceText(json.getString("text"));
                    } catch (JSONException e) {
                        // won't happen
                    }
                }
                if (BuildConfig.IS_BETA && script.name.equals("external")) {
                    String s = FileUtils.readFileContent(new File("/sdcard/LightningLauncher/tmp/script"));
                    if (s != null) {
                        script.setSourceText(s);
                    }
                }
            }
        } else {
            for (Script s : BUILTINS) {
                if (s.id == id) {
                    script = s;
                    break;
                }
            }
        }

        mScripts.put(id, script);

        return script;
    }

    public Script getOrLoadScript(String path, String name) {
        if(path != null) {
            path = sanitizeRelativePath(path);
        }
        // look in loaded scripts first
        Script script = getScriptByPathAndName(path, name);
        if(script != null) {
            return script;
        }

        // too bad, load all scripts...
        getAllScriptMatching(Script.FLAG_ALL);

        script = getScriptByPathAndName(path, name);

        // try again
        return script;
    }

    private Script getScriptByPathAndName(String path, String name) {
        for (int i = mScripts.size() - 1; i >= 0; i--) {
            Script script = mScripts.valueAt(i);
            if (script.name.equals(name) && (path == null || script.getRelativePath().equals(path))) {
                return script;
            }
        }

        return null;
    }

    public void saveScript(Script script) {
        boolean newScript = mScripts.get(script.id) == null;
        script.compiledScript = null;
        script.compiledFunction = null;
        mScripts.put(script.id, script);

        switch (script.getType()) {
            case Script.TYPE_FILE:
                File file = script.getFile();
                File directory = file.getParentFile();
                if(!directory.exists()) {
                    directory.mkdirs();
                }
                mScriptFiles.put(script.id, file);
                try {
                    JSONObject json = JsonLoader.toJSONObject(script, null);
                    FileUtils.saveStringToFile(json.toString(), file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case Script.TYPE_TARGET:
                if (!newScript) {
                    int itemId = script.getSourceItemId();
                    Page page = mLightningEngine.getOrLoadPage(Utils.getPageForItemId(itemId));
                    Item item = page.findItemById(itemId);
                    boolean changed = false;
                    for (int i = mScripts.size() - 1; i >= 0; i--) {
                        Script s = mScripts.valueAt(i);
                        if (s.getSourceItemId() == itemId) {
                            Binding[] bindings = item.getItemConfig().bindings;
                            for (Binding b : bindings) {
                                if (script.getSourceTarget().equals(b.target)) {
                                    if (!b.formula.equals(script.getSourceText())) {
                                        b.formula = script.getSourceText();

                                        // remove the script, it will be recreated when calling VariableManager.updateBindings later
                                        mScripts.remove(script.id);
                                        changed = true;
                                    }
                                }
                            }
                        }
                    }
                    if (changed) {
                        item.notifyBindingsChanged(true);
                    }
                }
                break;
        }
    }

    public void deleteScript(Script script) {
        int id = script.id;
        mScripts.remove(id);
        if (script.getType() == Script.TYPE_FILE) {
            File scriptFile = getScriptFile(id);
            if(scriptFile != null) {
                scriptFile.delete();
                removeEmptyDirectories(scriptFile.getParentFile());
                mScriptFiles.remove(id);
            }
        }
    }

    public Script importScript(Script script) {
        int newId = findFreeScriptId();
        File file;
        if(script.getType() == Script.TYPE_FILE) {
            file = new File(getScriptsDir()+script.getRelativePath()+"/"+newId);
        } else {
            file = null;
        }
        Script copy = new Script(this, script.getType(), Script.NO_ID, null, null, file);
        copy.copyFrom(script);
        copy.id = newId;
        saveScript(copy);
        return copy;
    }

    //    public boolean doesScriptExist(File base_dir, String name) {
    //        return new File(base_dir, SCRIPT_DIR+"/"+name).exists();
    //    }

    public int findFreeScriptId() {
        for (int i = 0; i < 10000; i++) {
            if (mScriptFiles.get(i) == null) {
                return i;
            }
        }

        return -1;
    }

    private int getNextTranscientId() {
        mNextTranscientId--;
        return mNextTranscientId;
    }

    public ArrayList<Script> getAllScriptMatching(int criteria) {
        long t1 = 0;
        if (BuildConfig.IS_BETA) {
            Log.i("LL", "ScriptManager.getAllScriptMatching");
            t1 = SystemClock.uptimeMillis();
        }

        ArrayList<Script> scripts = new ArrayList<Script>();

        loadScriptsInDirectory(scripts, getScriptsDir(), criteria);

        Collections.sort(scripts, new Comparator<Script>() {
            @Override
            public int compare(Script lhs, Script rhs) {
                return Utils.sItemNameCollator.compare(lhs.name, rhs.name);
            }
        });

        if (BuildConfig.IS_BETA) {
            Log.i("LL", "  " + scripts.size() + " scripts loaded in " + (SystemClock.uptimeMillis() - t1));
        }

        return scripts;
    }

    public String getRelativePath(File file) {
        String path = file.getAbsolutePath().substring(getScriptsDir().getAbsolutePath().length());
        if(path.equals("")) {
            path = "/";
        }
        return path;
    }

    public static String sanitizeRelativePath(String path) {
        if(path == null) {
            return "/";
        } else if(!path.startsWith("/")) {
            return "/" + path;
        } else if(path.startsWith(".")) {
            return "/";
        } else {
            return path;
        }
    }

    private void loadScriptsInDirectory(ArrayList<Script> scripts, File directory, int criteria) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    loadScriptsInDirectory(scripts, f, criteria);
                } else {
                    int id;
                    Script script;
                    try {
                        id = Integer.parseInt(f.getName());
                        script = getOrLoadScript(id);
                        if (criteria == Script.FLAG_ALL || (script.flags & criteria) == criteria) {
                            scripts.add(script);
                        }
                    } catch (NumberFormatException e) {
                        // pass, wrong file
                    }
                }
            }
        }
    }

    private void collectAllScriptsPaths(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    collectAllScriptsPaths(f);
                } else {
                    try {
                        int id = Integer.parseInt(f.getName());
                        mScriptFiles.put(id, f);
                    } catch (NumberFormatException e) {
                        // pass, wrong file
                    }
                }
            }
        }
    }

    public File getScriptsDir() {
        return new File(mLightningEngine.getBaseDir(), SCRIPT_DIR);
    }

    public File getScriptFile(int id) {
        return mScriptFiles.get(id);
    }

    public void clear() {
        mScripts.clear();
    }

    private boolean hasScriptWithName(String name) {
        int count = mScripts.size();
        for (int i = 0; i < count; i++) {
            Script script = mScripts.valueAt(i);
            if (script.name.equals(name)) {
                return true;
            }
        }

        return false;
    }

    public String getDefaultScriptName(Context context) {
        String default_name = context.getString(R.string.sc_untitled);
        if (!hasScriptWithName(default_name)) {
            return default_name;
        }

        for (int i = 1; i < 10000; i++) {
            String name = default_name + " " + i;
            if (!hasScriptWithName(name)) {
                return name;
            }
        }

        return default_name;
    }

    public Script createScriptForBinding(ItemView itemView, Binding binding) {
        Item item = itemView.getItem();
        String name = "Binding for " + item.formatForDisplay(true, 20) + ": " + Property.getByName(binding.target).getLabel();
        Script script = new Script(this, Script.TYPE_TARGET, getNextTranscientId(), name, binding.formula, null);
        script.setSourceItemId(item.getId());
        script.setSourceTarget(binding.target);
        saveScript(script);

        return script;
    }

    public Script createScriptForSetVariable(Item item, String text) {
        String name = "Binding for 'set variable'";
        if (item != null) name += " item " + item.formatForDisplay(true, 20);
        Script script = new Script(this, Script.TYPE_SET_VARIABLE, getNextTranscientId(), name, text, null);
        saveScript(script);

        return script;
    }

    public Script createScriptForFile(String name, String relativePath) {
        int id = findFreeScriptId();
        Script script = new Script(this, Script.TYPE_FILE, id, name, null, new File(getScriptsDir() + relativePath+"/"+id));
        mScriptFiles.put(script.id, script.getFile());
        return script;
    }

    public void moveScript(Script script, String toRelativePath) {
        toRelativePath = sanitizeRelativePath(toRelativePath);

        // build the new file and create directories as needed
        File toDir = new File(getScriptsDir() + toRelativePath);
        File toFile = new File(toDir, String.valueOf(script.id));
        if(!toDir.exists()) {
            toDir.mkdirs();
        }

        File fromFile = script.getFile();
        if(fromFile.equals(toFile)) {
            return;
        }

        fromFile.renameTo(toFile);
        script.setFile(toFile);

        // prune empty directories
        removeEmptyDirectories(fromFile.getParentFile());

        mScriptFiles.put(script.id, toFile);
    }

    private void removeEmptyDirectories(File directory) {
        File scriptsDir = getScriptsDir();
        while(!directory.equals(scriptsDir)) {
            String[] list = directory.list();
            if(list != null && list.length > 0) {
                break;
            }
            File parent = directory.getParentFile();
            directory.delete();
            directory = parent;
        }
    }
}
