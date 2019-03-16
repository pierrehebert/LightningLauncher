package net.pierrox.lightning_launcher.script.api;

import java.lang.reflect.Field;

import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.configuration.PageConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfigStylable;
import net.pierrox.lightning_launcher.data.*;

import android.util.Pair;

/**
 * Access to container and item properties (aka settings).
 * This object provides a way to query for configuration options as well as a mean to update them (see {@link #edit()}).
 * Properties are accessed through their name. There are different property classes:
 * <ul>
 * <li>container properties: options that can be seen in the desktop, folder and panel settings screen</li>
 * <li>item properties: options available to all objects (such as box properties or pin mode)</li>
 * <li>shortcut properties: options related to text/icon objects (including apps, shortcuts, folders and dynamic texts)</li>
 * <li>folder properties: options related to folder window (hence only for folder objects)</li>
 * </ul>
 * Available properties depends on the object from which this PropertySet is retrieved:
 * <ul>
 * <li>Container: contains properties for the container and default properties for items, shortcuts and folders.</li>
 * <li>Shortcuts, DynamicText:  contains item and shortcut properties</li>
 * <li>Folder: contains item, shortcut and folder properties</li>
 * <li>Page Indicator: item, shortcut and page indicator properties</li>
 * <li>Other objects: contains item properties only</li>
 * </ul>
 *
 * An instance of this object can be retrieved with {@link Item#getProperties()}, {@link Container#getProperties()} or {@link Configuration#getProperties()}.
 *
 * The list of supported properties can be found below. Behavior when setting a value for a property marked as read only is unspecified and can lead to data loss.
 * The same may appear when setting a value out of its bounds. These cases are currently not checked.
 *
 * <br><br><b>Container properties:</b>
 * <table>
 * 	<thead><tr><td>Name</td><td>Type</td><td>Access</td><td>Admissible values</td></tr></thead>
 * 	<tbody>
 * 		<tr><td><a href="/help/app/topic.php?id=74">newOnGrid</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=75">allowDualPosition</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=77">gridPColumnMode</a></td><td>string</td><td>Read/Write</td><td>AUTO|NUM|SIZE</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=78">gridPColumnNum</a></td><td>int</td><td>Read/Write</td><td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=79">gridPColumnSize</a></td><td>int</td><td>Read/Write</td><td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=77">gridPRowMode</a></td><td>string</td><td>Read/Write</td><td>AUTO|NUM|SIZE</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=78">gridPRowNum</a></td><td>int</td><td>Read/Write</td><td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=79">gridPRowSize</a></td><td>int</td><td>Read/Write</td><td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=77">gridLColumnMode</a></td><td>string</td><td>Read/Write</td><td>AUTO|NUM|SIZE</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=78">gridLColumnNum</a></td><td>int</td><td>Read/Write</td><td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=79">gridLColumnSize</a></td><td>int</td><td>Read/Write</td><td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=77">gridLRowMode</a></td><td>string</td><td>Read/Write</td><td>AUTO|NUM|SIZE</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=78">gridLRowNum</a></td><td>int</td><td>Read/Write</td><td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=79">gridLRowSize</a></td><td>int</td><td>Read/Write</td><td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=">gridPL</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=59">gridLayoutModeHorizontalLineColor</a></td><td>int</td><td>Read/Write</td><td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=60">gridLayoutModeHorizontalLineThickness</a></td><td>float</td><td>Read/Write</td><td>&gt;=0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=61">gridLayoutModeVerticalLineColor</a></td><td>int</td><td>Read/Write</td><td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=62">gridLayoutModeVerticalLineThickness</a></td><td>float</td><td>Read/Write</td><td>&gt;=0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=63">gridAbove</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 *
 * 		<tr><td><a href="/help/app/topic.php?id=53">bgSystemWPScroll</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=54">bgSystemWPWidth</a></td><td>int</td><td>Read/Write</td><td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=55">bgSystemWPHeight</a></td><td>int</td><td>Read/Write</td><td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=57">bgColor</a></td><td>int</td><td>Read/Write</td><td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=97">statusBarHide</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=109">statusBarColor</a></td><td>int</td><td>Read/Write</td><td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=138">navigationBarColor</a></td><td>int</td><td>Read/Write</td><td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=138">statusBarOverlap</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=138">navigationBarOverlap</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=96">screenOrientation</a></td><td>string</td><td>Read/Write</td><td>AUTO|PORTRAIT|LANDSCAPE|SYSTEM</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=84">scrollingDirection</a></td><td>string</td><td>Read/Write</td><td>AUTO|X|Y|XY|NONE</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=86">overScrollMode</a></td><td>string</td><td>Read/Write</td><td>DECELERATE|BOUNCE|NONE</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=90">noDiagonalScrolling</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=91">pinchZoomEnable</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=87">snapToPages</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=89">fitDesktopToItems</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=10000">autoExit</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=111">rearrangeItems</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=110">swapItems</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=76">freeModeSnap</a></td><td>string</td><td>Read/Write</td><td>NONE|CENTER|EDGE|CENTER_EDGE</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=139">useDesktopSize</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=146">noScrollLimit</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=161">wrapX</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=161">wrapY</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=10001">iconPack</a></td><td>string</td><td>Read/Write</td><td>package name or null (see {@link Container#applyIconPack(boolean)})</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=512">lwpStdEvents</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 *  	<tr><td colspan="4">Event handlers:</td></tr>
 *  	<tr><td>homeKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>menuKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>longMenuKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>backKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>longBackKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>searchKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>bgTap</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>bgDoubleTap</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>bgLongTap</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>swipeLeft</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>swipeRight</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>swipeUp</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>swipeDown</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>swipe2Left</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>swipe2Right</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>swipe2Up</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>swipe2Down</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>orientationPortrait</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>orientationLandscape</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>posChanged</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>load</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>paused</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>resumed</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>itemAdded</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>itemRemoved</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td><a href="/help/app/topic.php?id=204">menu</a></td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 	    <tr><td colspan="4">App Drawer only:</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=157">adHideActionBar</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=159">adDisplayABOnScroll</a></td><td>boolean</td><td>Read/Write</td><td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=190">adDisplayedModes</a></td><td>int</td><td>Read/Write</td><td>bitfield</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=194">adActionBarTextColor</a></td><td>int</td><td>Read/Write</td><td>argb color</td></tr>
 * 	</tbody>
 * </table>
 *
 * <br><br><b>Item properties:</b>
 * <table>
 * 	<thead><tr><td>Name</td><td>Type</td><td>Access</td><td>Admissible values</td></tr></thead>
 * 	<tbody>
 * 		<tr><td><a href="/help/app/topic.php?id=">i.box</a></td>   <td>{@link Box}</td>  <td>Read/Write through {@link net.pierrox.lightning_launcher.script.api.PropertyEditor#getBox(String)}</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=143">i.rotate</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=142">i.selectionEffect</a></td>   <td>string</td>  <td>Read/Write</td>  <td>PLAIN|HOLO|MATERIAL</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=169">i.selectionEffectMask</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=42">i.enabled</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=43">i.alpha</a></td>   <td>int</td>  <td>Read/Write</td>  <td>0..255</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=108">i.pinMode</a></td>   <td>string</td>  <td>Read/Write</td>  <td>NONE|XY|X|Y</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=44">i.filterTransformed</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=">i.onGrid</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=198">i.hardwareAccelerated</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=199">i.launchAnimation</a></td>   <td>string</td>  <td>Read/Write</td>  <td>NONE|FADE|SYSTEM|SLIDE_UP|SLIDE_DOWN|SLIDE_LEFT|SLIDE_RIGHT|SCALE_CENTER</td></tr>
 * 	  	<tr><td colspan="4">Event handlers:</td></tr>
 *  	<tr><td>i.tap</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>i.longTap</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>i.swipeLeft</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>i.swipeRight</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>i.swipeUp</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>i.swipeDown</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>i.touch</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>i.paused</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td>i.resumed</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 *      <tr><td><a href="/help/app/topic.php?id=204">i.menu</a></td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>

 * 	</tbody>
 * </table>
 *
 * <br><br><b>Shortcut properties:</b>
 * <table>
 * 	<thead><tr><td>Name</td><td>Type</td><td>Access</td><td>Admissible values</td></tr></thead>
 * 	<tbody>
 * 		<tr><td><a href="/help/app/topic.php?id=1">s.labelVisibility</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=7">s.labelFontColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=8">s.selectionColorLabel</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=9">s.focusColorLabel</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=3">s.labelFontSize</a></td>   <td>float</td>  <td>Read/Write</td>  <td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=5">s.labelFontTypeFace</a></td>   <td>string</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=4">s.labelFontStyle</a></td>   <td>string</td>  <td>Read/Write</td>  <td>NORMAL|ITALIC|BOLD|BOLD_ITALIC</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=6">s.labelMaxLines</a></td>   <td>int</td>  <td>Read/Write</td>  <td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=15">s.iconVisibility</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=200">s.iconSizeMode</a></td>   <td>string</td>  <td>Read/Write</td><td>STANDARD|REAL|FULL_SCALE</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=19">s.iconScale</a></td>   <td>float</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=29">s.iconReflection</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=30">s.iconReflectionOverlap</a></td>   <td>float</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=31">s.iconReflectionSize</a></td>   <td>float</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=32">s.iconReflectionScale</a></td>   <td>float</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=18">s.iconFilter</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=34">s.labelVsIconPosition</a></td>   <td>string</td>  <td>Read/Write</td>  <td>LEFT|TOP|RIGHT|BOTTOM</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=33">s.labelVsIconMargin</a></td>   <td>int</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=10">s.labelShadow</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=11">s.labelShadowRadius</a></td>   <td>float</td>  <td>Read/Write</td>  <td>&gt;=0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=12">s.labelShadowOffsetX</a></td>   <td>float</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=13">s.labelShadowOffsetY</a></td>   <td>float</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=14">s.labelShadowColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=28">s.iconEffectScale</a></td>   <td>float</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=162">s.iconColorFilter</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 	</tbody>
 * </table>
 *
 * <br><br><b>Folder properties:</b>
 * <table>
 * 	<thead><tr><td>Name</td><td>Type</td><td>Access</td><td>Admissible values</td></tr></thead>
 * 	<tbody>
 * 		<tr><td><a href="/help/app/topic.php?id=65">f.titleVisibility</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=66">f.titleFontColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=67">f.titleFontSize</a></td>   <td>float</td>  <td>Read/Write</td>  <td>&gt;0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=68">f.animationIn</a></td>   <td>string</td>  <td>Read/Write</td>  <td>NONE|OPEN_CLOSE|SLIDE_FROM_LEFT|SLIDE_FROM_RIGHT|SLIDE_FROM_TOP|SLIDE_FROM_BOTTOM</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=69">f.animationOut</a></td>   <td>string</td>  <td>Read/Write</td>  <td>NONE|OPEN_CLOSE|SLIDE_FROM_LEFT|SLIDE_FROM_RIGHT|SLIDE_FROM_TOP|SLIDE_FROM_BOTTOM</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=130">f.animFade</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=17">f.iconStyle</a></td>   <td>string</td>  <td>Read/Write</td>  <td>NORMAL|GRID_2_2|STACK</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=93">f.autoClose</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=136">f.closeOther</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=124">f.wAH</a></td>   <td>string</td>  <td>Read/Write</td>  <td>LEFT|CENTER|RIGHT|CUSTOM</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=125">f.wAV</a></td>   <td>string</td>  <td>Read/Write</td>  <td>TOP|CENTER|BOTTOM|CUSTOM</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=126">f.wX</a></td>   <td>int</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=127">f.wY</a></td>   <td>int</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=128">f.wW</a></td>   <td>int</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=129">f.wH</a></td>   <td>int</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=">f.box</a></td>   <td>{@link Box}</td>  <td>Read/Write through {@link net.pierrox.lightning_launcher.script.api.PropertyEditor#getBox(String)}</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=203">f.autoFindOrigin</a></td>   <td>boolean</td>  <td>Read/Write</td>  <td>true/false</td></tr>
 * 	</tbody>
 * </table>
 *
 * <br><br><b>Page Indicator properties:</b>
 * <table>
 * 	<thead><tr><td>Name</td><td>Type</td><td>Access</td><td>Admissible values</td></tr></thead>
 * 	<tbody>
 * 		<tr><td><a href="/help/app/topic.php?id=170">p.style</a></td>   <td>string</td>  <td>Read/Write</td>  <td>DOTS|RAW|MINIMAP|LINE_X|LINE_Y</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=171">p.rawFormat</a></td>   <td>string</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=172">p.dotsMarginX</a></td>   <td>int</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=173">p.dotsMarginY</a></td>   <td>int</td>  <td>Read/Write</td>  <td></td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=174">p.dotsOuterRadius</a></td>   <td>int</td>  <td>Read/Write</td>  <td>&gt;=0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=175">p.dotsInnerRadius</a></td>   <td>int</td>  <td>Read/Write</td>  <td>&gt;=0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=176">p.dotsOuterStrokeWidth</a></td>   <td>int</td>  <td>Read/Write</td>  <td>&gt;=0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=177">p.dotsOuterColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=178">p.dotsInnerColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=179">p.miniMapOutStrokeColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=180">p.miniMapOutFillColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=181">p.miniMapOutStrokeWidth</a></td>   <td>int</td>  <td>Read/Write</td>  <td>&gt;=0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=182">p.miniMapInStrokeColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=183">p.miniMapInFillColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=184">p.miniMapInStrokeWidth</a></td>   <td>int</td>  <td>Read/Write</td>  <td>&gt;=0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=185">p.lineBgWidth</a></td>   <td>int</td>  <td>Read/Write</td>  <td>&gt;=0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=186">p.lineBgColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=187">p.lineFgWidth</a></td>   <td>int</td>  <td>Read/Write</td>  <td>&gt;=0</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=188">p.lineFgColor</a></td>   <td>int</td>  <td>Read/Write</td>  <td>argb color</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=189">p.lineGravity</a></td>   <td>string</td>  <td>Read/Write</td>  <td>CENTER|LEFT_TOP|RIGHT_BOTTOM</td></tr>
 * 	</tbody>
 * </table>
 *
 * <br><br><b>Custom view properties:</b>
 * <table>
 * 	<thead><tr><td>Name</td><td>Type</td><td>Access</td><td>Admissible values</td></tr></thead>
 * 	<tbody>
 * 		<tr><td><a href="/help/app/topic.php?id=195">v.onCreate</a></td>   <td>string</td>  <td>Read/Write</td>  <td>id/data, where /data is optional</td></tr>
 * 		<tr><td><a href="/help/app/topic.php?id=196">v.onDestroy</a></td>   <td>string</td>  <td>Read/Write</td>  <td>id/data, where /data is optional</td></tr>
 * 	</tbody>
 * </table>
 *
 * <br><br><b>General configuration properties:</b>
 * <table>
 * 	<thead><tr><td>Name</td><td>Type</td><td>Access</td><td>Admissible values</td></tr></thead>
 * 	<tbody>
 * 		<tr><td>homeKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>menuKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>longMenuKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>backKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>longBackKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>searchKey</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>itemTap</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>itemLongTap</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>bgTap</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>bgDoubleTap</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>bgLongTap</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>swipeLeft</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>swipeRight</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>swipeUp</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>swipeDown</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>swipe2Left</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>swipe2Right</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>swipe2Up</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>swipe2Down</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>screenOn</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>screenOff</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>orientationPortrait</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>orientationLandscape</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>itemAdded</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>itemRemoved</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>menu</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 		<tr><td>startup</td><td>{@link net.pierrox.lightning_launcher.script.api.EventHandler}</td><td>Read/Write</td><td></td></tr>
 * 	</tbody>
 * </table>
 */
public class PropertySet {

    /*package*/ Lightning mLightning;
	private Object mScriptObject;
	/*package*/ enum Type {
		GLOBAL_CONFIG,
		CONTAINER,
		ITEM
	}

	private Type mType;
	
	/**
	 * @hide
	 */
	/*package*/ public PropertySet(Lightning lightning, Object script_object) {
		mLightning = lightning;
        mScriptObject = script_object;
		if(script_object instanceof GlobalConfig) {
			mType = Type.GLOBAL_CONFIG;
		} else if(script_object instanceof Container) {
			mType = Type.CONTAINER;
		} else {
			mType = Type.ITEM;
		}
	}

	/**
	 * @hide
	 */
	public Type getType() {
		return mType;
	}

	/**
	 * @hide
	 */
	/*package*/ Item getScriptItem() {
		return (Item) mScriptObject;
	}

	/**
	 * @hide
	 */
	/*package*/ Container getScriptContainer() {
		return (Container) mScriptObject;
	}
	
	public boolean getBoolean(String name) {
		return (Boolean)getProperty(name, Boolean.class);
	}
	
	public float getFloat(String name) {
		return (Float)getProperty(name, Float.class);
	}
	
	public int getInteger(String name) {
		return (Integer)getProperty(name, Integer.class);
	}
	
	public String getString(String name) {
		return (String)getProperty(name, String.class);
	}

	public EventHandler getEventHandler(String name) {
		EventAction ea = (EventAction)getProperty(name, EventAction.class);
        return ea==null ? null : new EventHandler(ea.clone());
	}

	/**
	 * Returns a box object allowing access to the box properties.
	 * This object does not allow modifications. Use the box object acquired through {@link PropertyEditor#getBox(String)} to alter values.
	 */
	public Box getBox(String name) {
		net.pierrox.lightning_launcher.data.Box box = (net.pierrox.lightning_launcher.data.Box) getProperty(name, net.pierrox.lightning_launcher.data.Box.class);
		return new Box(mLightning, box, name, null);
	}
	
	/*package*/ Object getProperty(String name, Class<?> expected_cls) {
		Pair<Object,String> c_p = getConfigObject(name, null);
		Object config = c_p.first;
		String property = c_p.second;
		
		try {
			Field f = config.getClass().getField(property);
			Object o = f.get(config);
			Class<?> cls = o.getClass();
			if(cls == expected_cls) {
				return o;
			} else if(expected_cls==String.class && cls.isEnum()) {
				return o.toString();
			} else {
				mLightning.scriptError("property "+name+" is a "+cls.getSimpleName()+", not a "+expected_cls.getSimpleName());
				return expected_cls.newInstance();
			}
		} catch (Exception e) {
			mLightning.scriptError("unknown property "+name);
			return null;
		}
	}
	
	/*package*/ Pair<Object,String> getConfigObject(String name, PageConfig cow_page_config) {
		if(mType == Type.GLOBAL_CONFIG) {
			return new Pair<>(mScriptObject, name);
		}

		Page page = null;
		
		if(mType == Type.CONTAINER) {
			page = getScriptContainer().getPage();
		}
		
		int dot = name.indexOf('.');
		if((page == null && dot != 1) || (page!=null && dot!=-1 && dot!=1)) {
			mLightning.scriptError("invalid property name");
			return null;
		}

		String property = name.substring(dot+1);
		Object config = null;
		if(page!=null && dot==-1) {
			config = page.config;
		} else {
			net.pierrox.lightning_launcher.data.Item item = mType==Type.ITEM ? getScriptItem().getItem() : null;
			switch(name.charAt(0)) {
			case 'i':
				if(page != null) {
					config = page.config.defaultItemConfig;
				} else {
					config = item.getItemConfig();
					if(cow_page_config!=null && cow_page_config.defaultItemConfig==config) {
						ItemConfig ic = new ItemConfig();
						ic.copyFrom((ItemConfig)config);
						item.setItemConfig(ic);
						config = ic;
					}
				}
				break;
				
			case 's':
				if(page != null) {
					config = page.config.defaultShortcutConfig;
				} else if(item instanceof ShortcutConfigStylable) {
                    ShortcutConfigStylable s = (ShortcutConfigStylable)item;
					config = s.getShortcutConfig();
					if(cow_page_config!=null && cow_page_config.defaultShortcutConfig==config) {
						ShortcutConfig sc = ((ShortcutConfig)config).clone();
						s.setShortcutConfig(sc);
						config = sc;
					}
				}
				break;
				
			case 'f':
				if(page != null) {
					config = page.config.defaultFolderConfig;
				} else if(item instanceof net.pierrox.lightning_launcher.data.Folder) {
					net.pierrox.lightning_launcher.data.Folder f = (net.pierrox.lightning_launcher.data.Folder)item;
					config = ((net.pierrox.lightning_launcher.data.Folder)item).getFolderConfig();
					if(cow_page_config!=null && cow_page_config.defaultFolderConfig==config) {
						FolderConfig fc = new FolderConfig();
						fc.copyFrom((FolderConfig)config);
						f.setFolderConfig(fc);
						config = fc;
					}
				}
				break;

            case 'p':
                if(page == null && item instanceof net.pierrox.lightning_launcher.data.PageIndicator) {
                    config = item;
                }
                break;

            case 'v':
                if(page == null && item instanceof net.pierrox.lightning_launcher.data.CustomView) {
                    config = item;
                }
                break;
			}

			if(config == null) {
				mLightning.scriptError("property name should start with i, s, f or p");
				return null;
			}
		}
		
		return new Pair<Object,String>(config, property);
	}
	
	/**
	 * Start to modify settings. Once all changes are made, don't forget to call {@link PropertyEditor#commit()} to validate changes.
	 * @return a PropertyEditor
	 */
	public PropertyEditor edit() {
		return new PropertyEditor(this);
	}
}
