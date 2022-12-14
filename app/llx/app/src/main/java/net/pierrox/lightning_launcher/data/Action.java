/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher.data;

public class Action {
    public static final int FLAG_TYPE_DESKTOP = 1;
    public static final int FLAG_TYPE_APP_DRAWER = 2;
    public static final int FLAG_TYPE_SCRIPT = 4;
    public static final int FLAG_ITEM = 8;

    public static final int CAT_NONE = 0;
    public static final int CAT_LAUNCH_AND_APPS = 1;
    public static final int CAT_NAVIGATION = 2;
    public static final int CAT_MENU_STATUS_BAR = 3;
    public static final int CAT_FOLDERS = 4;
    public static final int CAT_EDITION = 5;
    public static final int CAT_EXTERNAL = 6;
    public static final int CAT_ADVANCED = 7;

    public int action;
    public int label;
    public int category;
    public int flags;
    public int minSdkVersion;

    public Action(int action, int label, int category, int flags, int minSdkVersion) {
        this.action = action;
        this.label = label;
        this.category = category;
        this.flags = flags;
        this.minSdkVersion = minSdkVersion;
    }
}
