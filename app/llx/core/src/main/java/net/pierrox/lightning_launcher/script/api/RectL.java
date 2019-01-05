package net.pierrox.lightning_launcher.script.api;

import android.graphics.Rect;

/** A rectangle class. */
public class RectL {
	private int l, t, r, b;
	
	public RectL(int l, int t, int r, int b) {
		this.l = l;
		this.t = t;
		this.r = r;
		this.b = b;
	}

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

	public Rect toRect() {
		return new Rect(l, t, r, b);
	}
}