package net.pierrox.lightning_launcher.configuration;

import net.pierrox.lightning_launcher.data.Box;
import net.pierrox.lightning_launcher.data.JsonLoader;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;

import java.io.File;

public class FolderConfig extends JsonLoader {
	public enum FolderAnimation {
		NONE,
		OPEN_CLOSE,
        SLIDE_FROM_LEFT,
        SLIDE_FROM_RIGHT,
        SLIDE_FROM_TOP,
        SLIDE_FROM_BOTTOM
	}
	
	public enum FolderIconStyle {
		NORMAL,
		GRID_2_2,
		STACK
	}
	
//	public enum FolderBorderStyle {
//		NONE,
//		PLAIN,
//		NINE_PATCH
//	}
	
	public boolean titleVisibility=true;
	public int titleFontColor=Color.LTGRAY;
	public float titleFontSize=12f;
//	public String labelFontTypeFace=SYSTEM_FONT;
//	public FontStyle labelFontStyle=FontStyle.NORMAL;
//	public int labelMaxLines=1;
//    public boolean labelShadow=false;
//    public float labelShadowRadius=1;
//    public float labelShadowOffsetX=0;
//    public float labelShadowOffsetY=0;
//    public int labelShadowColor=Color.BLACK;

	public FolderAnimation animationIn=FolderAnimation.OPEN_CLOSE;
	public FolderAnimation animationOut=FolderAnimation.OPEN_CLOSE;
	public FolderIconStyle iconStyle=FolderIconStyle.GRID_2_2;
	public boolean autoClose=false;
	public boolean closeOther=false;
	public boolean animationGlitchFix=false;
	public boolean animFade=true;
	public boolean autoFindOrigin=true;

    public Box.AlignH wAH = Box.AlignH.CENTER;
    public Box.AlignV wAV = Box.AlignV.MIDDLE;
    public int wX, wY, wW, wH;

	public String box_s;
	
	public Box box;
	
	public FolderConfig() {
		box=new Box();
	}
	
	@Override
	public void loadFieldsFromJSONObject(JSONObject json, Object d) {
		super.loadFieldsFromJSONObject(json, d);
		FolderConfig fc_d = (FolderConfig)d;
		if(box_s!=null) {
			box.loadFromString(box_s, fc_d.box);
            box.bgFolder = fc_d.box.bgFolder;
		} else {
			box=fc_d.box;
		}
	}

    public void copyFrom(FolderConfig o) {
        super.copyFrom(o);
        box=new Box();
        box.loadFromString(o.box.toString(box), box);
        box.bgFolder = o.box.bgFolder;
    }
	
	public static FolderConfig readFromJsonObject(JSONObject o, FolderConfig d) throws JSONException {
		FolderConfig c=new FolderConfig();
		
		c.loadFieldsFromJSONObject(o, d);
		
		return c;
	}

    public void loadAssociatedIcons(File icon_dir, int id) {
        box.loadAssociatedDrawables(icon_dir, id, false);
    }
}
