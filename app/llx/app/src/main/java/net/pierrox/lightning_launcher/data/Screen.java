package net.pierrox.lightning_launcher.data;

import android.graphics.Bitmap;

public class Screen {
	public int page;
	public Bitmap icon;
	public String label;
	public boolean selected;
	
	public Screen(int page, Bitmap icon, String label, boolean selected) {
		this.page = page;
		this.icon = icon;
		this.label = label;
		this.selected = selected;
	}
	
}
