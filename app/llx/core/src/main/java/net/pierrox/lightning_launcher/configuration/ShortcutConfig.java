package net.pierrox.lightning_launcher.configuration;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.JsonLoader;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.views.MyTextView;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class ShortcutConfig extends JsonLoader {
    private static final PorterDuffXfermode sMaskXferMode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);

    private static final class CachedTypeface {
        String path;
        Typeface typeface;
    }
    private static CachedTypeface sCachedTypefaces[]=new CachedTypeface[15];

	public enum LabelVsIconPosition {
		LEFT,
		TOP,
		RIGHT,
		BOTTOM,
        CENTER
	}
	
	public enum FontStyle {
		NORMAL,
		BOLD,
		ITALIC,
		BOLD_ITALIC
	}

    public enum IconSizeMode {
        STANDARD,
        REAL,
        FULL_SCALE_RATIO,
        FULL_SCALE,
        NORMALIZED,
    }
	public boolean labelVisibility=true;
	public int labelFontColor=Color.WHITE;
	public float labelFontSize=12f;
	public String labelFontTypeFace=API.SHORTCUT_SYSTEM_FONT;
	public FontStyle labelFontStyle=FontStyle.NORMAL;
	public int labelMaxLines=1;
	public boolean iconVisibility=true;
	public IconSizeMode iconSizeMode=IconSizeMode.STANDARD;
	public float iconScale=1.0f;
	public boolean iconReflection=false;
	public float iconReflectionOverlap=0.3f;
	public float iconReflectionSize=1f;          // % of the reflection size, amount of visible reflection
	public float iconReflectionScale=1f; 
	public boolean iconFilter=false;
    public ShortcutConfig.LabelVsIconPosition labelVsIconPosition=LabelVsIconPosition.BOTTOM;
    public int labelVsIconMargin=4;
    public int selectionColorLabel=Color.WHITE;
    public int focusColorLabel=0xffccccff;
    
    public boolean labelShadow=true;
    public float labelShadowRadius=3;
    public float labelShadowOffsetX=1;
    public float labelShadowOffsetY=1;
    public int labelShadowColor=0xaa000000;
    
    public float iconEffectScale=1f;

    public int iconColorFilter=0xffffffff;

    public Drawable iconBack, iconOver, iconMask;
    
    public static ShortcutConfig readFromJsonObject(JSONObject o, ShortcutConfig d) throws JSONException {
		ShortcutConfig c=new ShortcutConfig();
		
		c.loadFieldsFromJSONObject(o, d);

        c.iconBack = d.iconBack;
        c.iconOver = d.iconOver;
        c.iconMask = d.iconMask;

		return c;
	}

    @Override
    public ShortcutConfig clone() {
        ShortcutConfig sc = new ShortcutConfig();
        sc.copyFrom(this);
        return sc;
    }

    @Override
    public void copyFrom(JsonLoader o) {
        super.copyFrom(o);
        ShortcutConfig sc = (ShortcutConfig)o;
        iconBack = sc.iconBack;
        iconOver = sc.iconOver;
        iconMask = sc.iconMask;
    }

    public void loadAssociatedIcons(File icon_dir, int id) {
        File f = getIconBackFile(icon_dir, id);
        if(f.exists()) iconBack=Utils.loadDrawable(f);
        f = getIconOverFile(icon_dir, id);
        if(f.exists()) iconOver=Utils.loadDrawable(f);
        f = getIconMaskFile(icon_dir, id);
        if(f.exists()) {
            SharedAsyncGraphicsDrawable d = Utils.loadDrawable(f);
            iconMask = d;
            if(d != null && d.getType() == SharedAsyncGraphicsDrawable.TYPE_BITMAP) {
                d.getPaint().setXfermode(sMaskXferMode);
            }
        }
    }

    public static File getIconBackFile(File icon_dir, int id) {
        return new File(icon_dir, (id== Item.NO_ID?"":id)+FileUtils.SUFFIX_ICON_BACK);
    }

    public static File getIconOverFile(File icon_dir, int id) {
        return new File(icon_dir, (id== Item.NO_ID?"":id)+FileUtils.SUFFIX_ICON_OVER);
    }

    public static File getIconMaskFile(File icon_dir, int id) {
        return new File(icon_dir, (id== Item.NO_ID?"":id)+FileUtils.SUFFIX_ICON_MASK);
    }

    public void applyFontStyleToTextView(MyTextView tv) {
        Typeface tf=getTypeFace(labelFontTypeFace);
        int s;
        switch(labelFontStyle) {
            case BOLD: s=Typeface.BOLD; break;
            case BOLD_ITALIC: s=Typeface.BOLD_ITALIC; break;
            case ITALIC: s=Typeface.ITALIC; break;
            default: s=Typeface.NORMAL; break;
        }
        tv.setTypeface(tf, s);
    }

    public void applyToTextView(MyTextView tv, ItemConfig ic) {
        tv.setTextColor(labelFontColor);
        tv.setTextSize(labelFontSize);
        applyFontStyleToTextView(tv);
        //tv.setIncludeFontPadding(false);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        if(labelMaxLines==1) {
            tv.setSingleLine();
        } else {
            tv.setSingleLine(false);
            tv.setMaxLines(labelMaxLines);
            int g;
            switch(ic.box.ah) {
                case LEFT: g= Gravity.LEFT; break;
                case RIGHT: g=Gravity.RIGHT; break;
                default: g=Gravity.CENTER; break;
            }
            tv.setGravity(g);
        }
        if(labelShadow) {
            tv.setShadowLayer(labelShadowRadius, labelShadowOffsetX, labelShadowOffsetY, labelShadowColor);
        }
        tv.setfixWidth(labelShadow || labelFontStyle== ShortcutConfig.FontStyle.ITALIC || labelFontStyle== ShortcutConfig.FontStyle.BOLD_ITALIC);
    }

    private static Typeface getTypeFace(String font_path) {
        if(API.SHORTCUT_SYSTEM_FONT.equals(font_path)) {
            return null;
        }

        if(API.SHORTCUT_ICON_FONT.equals(font_path)) {
            return LLApp.get().getIconsTypeface();
        }

        CachedTypeface ctf=null;
        int l=sCachedTypefaces.length;
        int i;
        for(i=0; i<l; i++) {
            ctf=sCachedTypefaces[i];
            if(ctf==null) break;
            if(ctf.path.equals(font_path)) return ctf.typeface;
        }

        if(i<l) {
            ctf=new CachedTypeface();
            ctf.path=font_path;
            try {
                ctf.typeface=Typeface.createFromFile(font_path);
                sCachedTypefaces[i]=ctf;
                return ctf.typeface;
            } catch(Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }
}