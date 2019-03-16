package net.pierrox.lightning_launcher.script.api;

import android.graphics.Rect;

/**
 * A rectangle class in the format left-top-rigth-bottom.
 *
 * An instance of this object can be created with {@link #RectL(Rect)} or {@link #RectL(int, int, int, int)}; or retrieved with {@link Item#getCell()} or {@link Container#getBoundingBox()}.
 */
public class RectL {
	private int l, t, r, b;
	
	public RectL(int l, int t, int r, int b) {
		this.l = l;
		this.t = t;
		this.r = r;
		this.b = b;
	}
	
	/**
	 * Copy constructor. Creates a copy of the RectL
	 * @param r the rectL which will be copied.
	 */
	public RectL(android.graphics.Rect r) {
		this(r.left, r.top, r.right, r.bottom);
	}

	public int getLeft() { return l; }

	public int getRight() { return r; }

	public int getTop() { return t; }

	public int getBottom() { return b; }
	
	public String toString() {
		return "["+l+","+t+","+r+","+b+"]";
	}
	
	/**
	 * Returns a java Rect with the same values.
	 * @return Java Rect object with the same values.
	 */
	public Rect toRect() {
		return new Rect(l, t, r, b);
	}
}