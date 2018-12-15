package net.pierrox.lightning_launcher.script;

import android.util.Pair;

import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.Utils;

import org.mozilla.javascript.Function;

import java.io.File;
import java.util.HashMap;

public class Script extends JsonLoader {
	public static final int NO_ID = -1;

    public static final int TYPE_FILE = 0;
    public static final int TYPE_BUILTIN = 1;
    public static final int TYPE_TARGET = 2;
    public static final int TYPE_SET_VARIABLE = 3;

	public static final int TARGET_NONE = -1;
	public static final int TARGET_DESKTOP = 0;
	public static final int TARGET_APP_DRAWER = 1;
	public static final int TARGET_LOCK_SCREEN = 2;
	public static final int TARGET_BACKGROUND = 3;

	public static final int FLAG_ALL = 0;
	public static final int FLAG_DISABLED = 1;
	public static final int FLAG_APP_MENU = 2;
	public static final int FLAG_ITEM_MENU = 4;
	public static final int FLAG_CUSTOM_MENU = 8;

    // public fields use for JSON serialization
	public int id = Script.NO_ID;
    public String name = "";
    public String mSourceText = "";
    public int flags = 0;
    public String tag = null;
    public HashMap<String,String> tags;

    private ScriptManager mScriptManager;
    private File mFile;
    private int mType;
    private int mSourceItemId;
    private String mSourceTarget;
    private String mProcessedText;
    public org.mozilla.javascript.Script compiledScript;
    public Function compiledFunction;

    /*package*/ Script(ScriptManager scriptManager, int type, int id, String name, String text, File file) {
        mScriptManager = scriptManager;
        mType = type;
        mFile = file;

        this.id = id;
        if(type == TYPE_BUILTIN) {
            this.name = "builtin" + id;
            this.mSourceText = "// Hello, I am a builtin script! I may be changed or deleted without prior notice. Have fun.\n\n" + text;
        } else {
            this.name = name;
            this.mSourceText = text == null ? "" : text;
        }
    }

    public boolean hasFlag(int flag) {
    	return (flags & flag) != 0;
    }
    
    public void setFlag(int flag, boolean on) {
    	if(on) {
    		flags |= flag;
    	} else {
    		flags &= ~flag;
    	}
    }

    /**
     * @return the script text that should be compiled
     */
    public String getScriptText() {
        return mProcessedText == null ? mSourceText : mProcessedText;
    }

    public void setSourceText(String text) {
        mSourceText = text;
    }

    /**
     * @return the script text that serve as the origin for this script (can possibly be processed before to be compiled)
     */
    public String getSourceText() {
        return mSourceText;
    }

    public void setProcessedText(String text) {
        mProcessedText = text;
    }
    
    @Override
    public String toString() {
    	return Utils.formatHex(id, 3)+": "+name;
    }

    public static Pair<Integer,String> decodeIdAndData(String id_and_data) {
        if(id_and_data == null) {
            return new Pair<>(Script.NO_ID, null);
        }
        // data can be either id or id/data
        int id;
        String data;
        int p = id_and_data.indexOf('/');
        if(p == -1) {
            id = Integer.valueOf(id_and_data);
            data = null;
        } else {
            id = Integer.valueOf(id_and_data.substring(0, p));
            data = id_and_data.substring(p + 1);
        }
        return new Pair<>(id, data);
    }

    public static String encodeIdAndData(int id, String data) {
        return data == null ? String.valueOf(id) : id + "/" + data;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        this.mType = type;
    }

    public int getSourceItemId() {
        return mSourceItemId;
    }

    public void setSourceItemId(int sourceItemId) {
        this.mSourceItemId = sourceItemId;
    }

    public String getSourceTarget() {
        return mSourceTarget;
    }

    public void setSourceTarget(String sourceTarget) {
        this.mSourceTarget = sourceTarget;
    }

    public File getFile() {
        return mFile;
    }

    /*package*/ void setFile(File file) {
        mFile = file;
    }

    public String getRelativePath() {
        return mScriptManager.getRelativePath(mFile.getParentFile());
    }

    public void setRelativePath(String relativePath) {
        mScriptManager.moveScript(this, relativePath);
    }
}
