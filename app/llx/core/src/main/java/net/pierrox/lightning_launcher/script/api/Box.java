package net.pierrox.lightning_launcher.script.api;


import net.pierrox.lightning_launcher.data.Utils;

/**
 * The Box object allows access to the corresponding item and folder window box properties.
 * A box is made of several areas: margins, borders, padding, content. 
 * Size of margins, borders and padding is configurable, not content (this is the remaining space). Colors of borders, and content is configurable, not margins and padding (always transparent).
 * Methods of the box object use a code to identify each area:
 * <ul>
 * <li>Margin left:<b>ml</b></li>
 * <li>Margin top:<b>mt</b></li>
 * <li>Margin right:<b>mr</b></li>
 * <li>Margin bottom:<b>mb</b></li>
 * <li>Border left:<b>bl</b></li>
 * <li>Border top:<b>bt</b></li>
 * <li>Border right:<b>br</b></li>
 * <li>Border bottom:<b>bb</b></li>
 * <li>Padding left:<b>pl</b></li>
 * <li>Padding top:<b>pt</b></li>
 * <li>Padding right:<b>pr</b></li>
 * <li>Padding bottom:<b>pb</b></li>
 * <li>Content:<b>c</b></li>
 * </ul>
 *
 * An instance of this object can be retrieved with {@link PropertyEditor#getBox(String)} or {@link PropertySet#getBox(String)}.
 *
 * Example of using the Box object to change item properties:
 * <pre>
 * var color = 0xff00ff00; // pure green
 * var editor = item.getProperties().edit();
 * var box = editor.getBox("i.box");
 * 
 * box.setColor("c", "ns", color); // set background color, for normal and selected states
 * 
 * box.setColor("bl,br,bt,bb", "ns", color); // set all borders color for normal and selected states.
 * 
 * editor.getBox("f.box").setColor("c", "n", color); // set folder background color
 * 
 * editor.commit();</pre>
 */
public class Box {
    /*package*/ static final char LIST_SEP = ';';
    /*package*/ static final char TOK_SEP = ':';

	private Lightning mLightning;
	private net.pierrox.lightning_launcher.data.Box mBox;
	private String mKey;
	private PropertyEditor mEditor;
    private String mEncodedBox;
	
	/*package*/ Box(Lightning lightning, net.pierrox.lightning_launcher.data.Box box, String key, PropertyEditor editor) {
		mLightning = lightning;
		mBox = box;
		mKey = key;
		mEditor = editor;
	}
	
	
	/**
	 * Set the size of a given list of areas.
	 * @param areas one or more areas as a list of comma separated area codes. @see {@link Box} for areas color codes.
	 * @param size in pixel
	 */
	public void setSize(String areas, int size) {
		if(mEditor == null) {
			notModifiableError();
		} else {
            if(areas == null) {
                return;
            }

            for(String area : areas.split(",")) {
                if(area.length() == 2) {
                    char c0 = area.charAt(0);
                    char c1 = area.charAt(1);
                    if(c1=='l' || c1=='t' || c1=='r' || c1=='b') {
                        String token = null;
                        if(c0=='m' || c0=='p') {
                            token = area+TOK_SEP+size;
                        } else if(c0=='b') {
                            token = "bs"+c1+TOK_SEP+size;
                        }
                        if(token != null) {
                            if(mEncodedBox == null) {
                                mEncodedBox = token;
                            } else {
                                mEncodedBox += LIST_SEP+token;
                            }
                        }
                    }
                }
            }

            mEditor.setString(mKey, mEncodedBox);
		}
	}
	
	/**
	 * Set the color of a given list of areas.
	 * @param areas one or more areas as a list of comma separated area codes. @see {@link Box} for areas color codes.
	 * @param modes a combination of "n" for normal, "s" for selected, and "f" for focused.
	 * @param color argb color value
	 */
	public void setColor(String areas, String modes, long color) {
		if(mEditor == null) {
			notModifiableError();
		} else {
            String color_string = Utils.formatHex(color, 8); // this is to avoid parse error because of long int. JavaScript requires long, colors need int
            for(String area : areas.split(",")) {
                String token = null;
                if("c".equals(area)) {
                    token = area + modes + ":" + color_string;
                } else if (area.length() == 2) {
                    char c0 = area.charAt(0);
                    char c1 = area.charAt(1);
                    if(c0=='b' && (c1=='l' || c1=='t' || c1=='r' || c1=='b')) {
                        token = "bc"+c1+modes+TOK_SEP+color_string;
                    }
                }

                if(token != null) {
                    if(mEncodedBox == null) {
                        mEncodedBox = token;
                    } else {
                        mEncodedBox += LIST_SEP+token;
                    }
                }
            }

            mEditor.setString(mKey, mEncodedBox);
		}
	}
	
	/**
	 * Set the horizontal and vertical box alignment.
	 * @param h horizontal alignment, one of LEFT, CENTER, RIGHT
	 * @param v vertical alignment, one of TOP, MIDDLE, BOTTOM
	 */
	public void setAlignment(String h,  String v) {
		if(mEditor == null) {
			notModifiableError();
		} else {
            String align = "ah"+TOK_SEP+h+LIST_SEP+"av"+TOK_SEP+v;

            if(mEncodedBox == null) {
                mEncodedBox = align;
            } else {
                mEncodedBox += LIST_SEP+align;
            }

            mEditor.setString(mKey, mEncodedBox);
		}
	}
	
	private void notModifiableError() {
		mLightning.scriptError("This box object does not allow modifications. Use a box object acquired through a PropertyEditor");
	}
	
	/**
	 * Returns the box horizontal alignment.
	 * @see #setAlignment(String, String) for available values
	 */
	public String getAlignmentH() {
		return mBox.ah.toString();
	}
	
	/**
	 * Returns the box vertical alignment.
	 * @see #setAlignment(String, String) for available values
	 */
	public String getAlignmentV() {
		return mBox.av.toString();
	}
	
	/**
	 * Returns the color of one area.
	 * @param area @see {@link Box} for areas color codes
	 * @param mode one of "n" for normal, "s" for selected, or "f" for focused
	 * @return a color corresponding to the given area, or an unspecified color if area or mode is invalid
	 */
	public int getColor(String area, String mode) {
		if("c".equals(area)) {
			// content
			if("s".equals(mode)) return mBox.ccs;
			if("f".equals(mode)) return mBox.ccf;
			else return mBox.ccn;
		} else {
			int shift;
			if("s".equals(mode)) shift = net.pierrox.lightning_launcher.data.Box.COLOR_SHIFT_S;
			if("f".equals(mode)) shift = net.pierrox.lightning_launcher.data.Box.COLOR_SHIFT_F;
			else shift = net.pierrox.lightning_launcher.data.Box.COLOR_SHIFT_N;
			int bc;
			if("bl".equals(area)) bc = net.pierrox.lightning_launcher.data.Box.BCL;
			else if("bt".equals(area)) bc = net.pierrox.lightning_launcher.data.Box.BCT;
			else if("br".equals(area)) bc = net.pierrox.lightning_launcher.data.Box.BCR;
			else bc = net.pierrox.lightning_launcher.data.Box.BCB;
			return mBox.border_color[shift + bc];
		}
	}
	
	/**
	 * Returns the size of one area.
	 * @param area @see {@link Box} for areas color codes
	 * @return the size in pixel of the given area, or 0 if area is not valid
	 */
	public int getSize(String area) {
		int i = getAreaIndex(area);
		if(i == -1) {
			return 0;
		} else {
			return mBox.size[i];
		}
	}

	/**
	 * Access to the internal box data. To be used with LLPreferenceBox.
     */
	public Object getBox() {
		return mBox;
	}
	
	/*package*/ static int getAreaIndex(String code) {
		if("ml".equals(code)) return net.pierrox.lightning_launcher.data.Box.ML;
		else if("mt".equals(code)) return net.pierrox.lightning_launcher.data.Box.MT;
		else if("mr".equals(code)) return net.pierrox.lightning_launcher.data.Box.MR;
		else if("mb".equals(code)) return net.pierrox.lightning_launcher.data.Box.MB;
		else if("bl".equals(code)) return net.pierrox.lightning_launcher.data.Box.BL;
		else if("bt".equals(code)) return net.pierrox.lightning_launcher.data.Box.BT;
		else if("br".equals(code)) return net.pierrox.lightning_launcher.data.Box.BR;
		else if("bb".equals(code)) return net.pierrox.lightning_launcher.data.Box.BB;
		else if("pl".equals(code)) return net.pierrox.lightning_launcher.data.Box.PL;
		else if("pt".equals(code)) return net.pierrox.lightning_launcher.data.Box.PT;
		else if("pr".equals(code)) return net.pierrox.lightning_launcher.data.Box.PR;
		else if("pb".equals(code)) return net.pierrox.lightning_launcher.data.Box.PB;
		else return -1;
	}
}
