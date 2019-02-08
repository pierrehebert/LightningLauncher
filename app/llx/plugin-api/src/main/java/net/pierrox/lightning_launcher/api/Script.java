package net.pierrox.lightning_launcher.api;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

/**
 * @author lukas
 * @since 08.02.19
 */
@Parcel(Parcel.Serialization.BEAN)
public class Script {
	public static final int NO_ID = -1;
	public static final int FLAG_ALL = 0;
	public static final int FLAG_DISABLED = 1;
	public static final int FLAG_APP_MENU = 2;
	public static final int FLAG_ITEM_MENU = 4;
	public static final int FLAG_CUSTOM_MENU = 8;
	private int id = NO_ID;
	private String text;
	private String name;
	private String path;
	private int flags;

	public Script(String text, String name, String path, int flags) {
		this.text = text;
		this.name = name;
		this.path = path;
		this.flags = flags;
	}

	@ParcelConstructor
	public Script(int id, String text, String name, String path, int flags) {
		this.id = id;
		this.text = text;
		this.name = name;
		this.path = path;
		this.flags = flags;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean hasFlag(int flag) {
		return (flags & flag) != 0;
	}

	public void setFlag(int flag, boolean on) {
		if (on) {
			flags |= flag;
		} else {
			flags &= ~flag;
		}
	}

	public int getFlags() {
		return flags;
	}
}
