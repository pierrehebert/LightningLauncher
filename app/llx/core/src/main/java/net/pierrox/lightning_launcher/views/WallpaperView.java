package net.pierrox.lightning_launcher.views;

import net.pierrox.lightning_launcher.configuration.PageConfig;

import java.io.File;

public interface WallpaperView {
    void setVisibility(int visibility);
    void configure(int page, final File file, int tint_color, PageConfig.ScaleType scaleType);
}