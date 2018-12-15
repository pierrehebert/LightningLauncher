package net.pierrox.lightning_launcher.data;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import net.pierrox.lightning_launcher.util.AnimationDecoder;
import net.pierrox.lightning_launcher.views.Graphics;
import net.pierrox.lightning_launcher.views.SharedAsyncGraphicsDrawable;
import net.pierrox.lightning_launcher.views.svg.SvgDrawable;

import java.io.File;

public class Box implements SharedAsyncGraphicsDrawable.GraphicsProvider {
	public enum AlignH {
		LEFT,
		CENTER,
		RIGHT,
        CUSTOM
	}
	
	public enum AlignV {
		TOP,
		MIDDLE,
		BOTTOM,
        CUSTOM
	}
	
	public static final int ML=0;
	public static final int MT=1;
	public static final int MR=2;
	public static final int MB=3;
	public static final int BL=4;
	public static final int BT=5;
	public static final int BR=6;
	public static final int BB=7;
	public static final int PL=8;
	public static final int PT=9;
	public static final int PR=10;
	public static final int PB=11;
	
	public static final int BCL=0;
	public static final int BCT=1;
	public static final int BCR=2;
	public static final int BCB=3;
	public static final int COLOR_SHIFT_N=0;
	public static final int COLOR_SHIFT_S=4;
	public static final int COLOR_SHIFT_F=8;

	public int[] size;
	public int[] border_color;
	
	// color content normal/pressed/focused
	public int ccn=Color.TRANSPARENT;
	public int ccs=0xffffffff;
	public int ccf=0x808080ff;
	
	// align h/v
	public AlignH ah=AlignH.CENTER;
	public AlignV av=AlignV.MIDDLE;

    public Drawable bgNormal;
    public Drawable bgSelected;
    public Drawable bgFocused;
    public Drawable bgFolder;

	public Box() {
		size = new int[12];
		border_color = new int[12];
		for(int i=0; i<border_color.length; i++) border_color[i] = Color.WHITE;
	}
	
	public void loadFromString(String s, Box d) {
		String[] e=s.split(":");

		try {
			loadArray(e, 0, size, d.size);
			loadArray(e, 12, border_color, d.border_color);
			
			String v;
			v=getStringAt(e, 24); ccn=v==null ? d.ccn : Integer.parseInt(v);
			v=getStringAt(e, 25); ccs=v==null ? d.ccs : Integer.parseInt(v);
			v=getStringAt(e, 26); ccf=v==null ? d.ccf : Integer.parseInt(v);
			
			v=getStringAt(e, 27); ah=v==null ? d.ah : AlignH.valueOf(v);
			v=getStringAt(e, 28); av=v==null ? d.av : AlignV.valueOf(v);
		} catch(Exception e1) {
			// pass
		}
	}

    public void loadAssociatedDrawables(File icon_dir, int id, boolean for_item) {
        File f;
        if(for_item) {
            f = getBoxBackgroundNormal(icon_dir, id);
            if(f.exists()) bgNormal = new SharedAsyncGraphicsDrawable(this, f, true);

            f = getBoxBackgroundSelected(icon_dir, id);
            if(f.exists()) bgSelected = new SharedAsyncGraphicsDrawable(this, f, true);

            f = getBoxBackgroundFocused(icon_dir, id);
            if(f.exists()) bgFocused = new SharedAsyncGraphicsDrawable(this, f, true);
        } else {
            f = getBoxBackgroundFolder(icon_dir, id);
            if(f.exists()) bgFolder = new SharedAsyncGraphicsDrawable(this, f, true);
        }
    }

    @Override
    public Graphics provideGraphics(SharedAsyncGraphicsDrawable sbd, Object data, int max_width, int max_height) {
		File file = (File) data;
		Graphics graphics = null;
        if(Utils.isGifFile(file)) {
			AnimationDecoder animationDecoder = Utils.loadGifDecoder(file);
			graphics = animationDecoder == null ? null : new Graphics(animationDecoder, 0, 0);
		} else if(Utils.isSvgFile(file)) {
			SvgDrawable svgDrawable = new SvgDrawable(file);
			graphics = new Graphics(svgDrawable);
		}
		if(graphics == null) {
			Bitmap bitmap = Utils.loadBitmap((File) data, 0, max_width, max_height);
			graphics = bitmap == null ? null : new Graphics(bitmap);
		}
		return graphics;
    }

	@Override
	public boolean composeGraphics(Bitmap baseIcon, Bitmap finalIcon) {
		return false;
	}

	public static File getBoxBackgroundNormal(File icon_dir, int id) {
        return new File(icon_dir, (id== Item.NO_ID?"":id)+FileUtils.SUFFIX_BOX_BG_NORMAL);
    }

    public static File getBoxBackgroundSelected(File icon_dir, int id) {
        return new File(icon_dir, (id== Item.NO_ID?"":id)+FileUtils.SUFFIX_BOX_BG_SELECTED);
    }

    public static File getBoxBackgroundFocused(File icon_dir, int id) {
        return new File(icon_dir, (id== Item.NO_ID?"":id)+FileUtils.SUFFIX_BOX_BG_FOCUSED);
    }

    public static File getBoxBackgroundFolder(File icon_dir, int id) {
        return new File(icon_dir, (id== Item.NO_ID?"":id)+FileUtils.SUFFIX_BOX_BG_FOLDER);
    }

	private static String getStringAt(String e[], int i) {
		if(i<e.length) {
			String v = e[i];
			return v.length() == 0 ? null : v;
		} else {
			return null;
		}
	}
	
	private void loadArray(String[] e, int offset, int[] dst, int[] dst_def) {
		int n = dst.length;
		for(int i=0; i<n; i++) {
			String v = getStringAt(e, i+offset);
			dst[i] = v==null ? dst_def[i] : Integer.parseInt(v);
		}
	}

	public String toString(Box d) {
		sTmpStringBuffer.setLength(0);

		appendArray(size, d==null ? null : d.size);
		
		appendArray(border_color, d==null ? null : d.border_color);
		
		if(d==null || d.ccn!=ccn) sTmpStringBuffer.append(ccn);
		sTmpStringBuffer.append(':');
		if(d==null ||d.ccs!=ccs) sTmpStringBuffer.append(ccs);
		sTmpStringBuffer.append(':');
		if(d==null ||d.ccf!=ccf) sTmpStringBuffer.append(ccf);
		sTmpStringBuffer.append(':');
		
		if(d==null ||d.ah!=ah) sTmpStringBuffer.append(ah.toString());
		sTmpStringBuffer.append(':');
		if(d==null ||d.av!=av) sTmpStringBuffer.append(av.toString());
		sTmpStringBuffer.append(':');
		
		return sTmpStringBuffer.toString();
	}
	
	private void appendArray(int[] src, int[] src_def) {
		int n = src.length;
		for(int i=0; i<n; i++) {
			int v = src[i];
			if(src_def==null || v!=src_def[i]) sTmpStringBuffer.append(v);
			sTmpStringBuffer.append(':');
		}
	}
	
	private static StringBuffer sTmpStringBuffer=new StringBuffer(200);
}
