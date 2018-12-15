/*
Pierre Hébert - Week Day 11/2016
Based on the script of Jappie Toutenhoofd https://plus.google.com/+JappieToutenhoofd/posts/P1aKQjkPMij
Compatible with Lightning Launcher v14b6+.
This software is provided as is. Public domain.
You are free to use, duplicate and modify it without restriction.
*/

var screen = getActiveScreen();
var context = screen.getContext();
var version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode%1000;
if(version < 305) {
    alert("You need Lightning Launcher V14b6 or later to use this script");
    return;
}

var MY_PKG = "net.pierrox.lightning_launcher.llscript.calendar_icon";

// install (or update) a script given its id in the package, and its clear name in the launcher data
function installScript(id, name) {
    // use our package name to classify scripts
    var path = '/'+MY_PKG.replace(/\./g, '/');

    // load the script (if any) among the existing ones
    var script = getScriptByPathAndName(path, name);

    // load the script text from the package
    var script_text = loadRawResource(MY_PKG, id);

    if(script == null) {
        // script not found: install it
        script = createScript(path, name, script_text , 0);
    } else {
        // the script already exists: update its text
        script.setText(script_text);
    }

    return script;
}

bindClass("java.lang.Runnable");
bindClass("java.util.Collections");
bindClass("java.util.ArrayList");
bindClass("android.content.pm.PackageManager");
bindClass("android.content.pm.ResolveInfo");
bindClass("android.os.AsyncTask");
bindClass("android.os.Handler");
bindClass("android.app.AlertDialog");
bindClass("android.app.ProgressDialog");
bindClass("android.view.LayoutInflater");
bindClass("android.widget.ListView");
bindClass("android.widget.ArrayAdapter");
bindClass("org.xmlpull.v1.XmlPullParser");


var pd = new ProgressDialog(context);
pd.setMessage("Please wait...");
pd.setCancelable(false);

var handler = new Handler();
var delayedProgressDisplay = new Runnable({
   run: function() {
       pd.show();
   }
});
handler.postDelayed(delayedProgressDisplay, 200)

new AsyncTask({
    doInBackground: function(params) {
        return retrieveCalendarIcons();
    },

    onPostExecute: function(result) {
        if(pd.isShowing()) {
            pd.dismiss();
        } else {
            handler.removeCallbacks(delayedProgressDisplay)
        }

        if(result.size() == 0) {
            showNoCalendarIconDialog();
        } else {
            showCalendarIconPicker(result);
        }
    }
}).execute(null);

return;




function retrieveCalendarIcons() {
    var pm = context.getPackageManager();

    var filter = new Intent("org.adw.launcher.icons.ACTION_PICK_ICON");
    var ris = pm.queryIntentActivities(filter, 0);

    Collections.sort(ris, new ResolveInfo.DisplayNameComparator(pm));
    var iconPacks = new ArrayList();
    for(var i=0; i<ris.size(); i++){
        var ri = ris.get(i);
        var pkg = ri.activityInfo.packageName;
        if(pkg != "net.pierrox.lightning_launcher_extreme") {


            var iconpackContext = context.createPackageContext(pkg, 0);
            var iconPackRsrc = iconpackContext.getResources();
            var appfilterId = iconPackRsrc.getIdentifier("appfilter", "xml", pkg);
            if(appfilterId != 0) {
                var parser = iconPackRsrc.getXml(appfilterId);

                if(parser != null) {
                    var prefixes = {};

                    var eventType = parser.next();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            var name = parser.getName();
                            if (name.equals("calendar")) {
                                var prefix = parser.getAttributeValue(null, "prefix");
                                prefixes[prefix] = 0; // just use as a set, value does not matter
                            }
                        }
                        eventType = parser.next();
                    }

                    var keys = Object.keys(prefixes);
                    var iconPackName = ri.loadLabel(pm);
                    for(var k=0; k<keys.length; k++) {
                        var prefix = keys[k];
                        var iconId = iconPackRsrc.getIdentifier(prefix+"31", "drawable", pkg);
                        iconPacks.add({
                                label: iconPackName,
                                prefix: prefix,
                                icon: iconPackRsrc.getDrawable(iconId),
                                package: pkg,
                        });
                    }
                }
            }
        }
    }

    return iconPacks;
}

function showNoCalendarIconDialog() {
    var builder=new AlertDialog.Builder(context);
    builder.setTitle("No calendar icon found");
    builder.setMessage("Would you like to install an icon pack providing dynamic calendar icons?");
    builder.setPositiveButton("Ok", {
        onClick:function(dialog,id) {
            screen.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=dynamic%20calendar%20icon")));
        }
    });
    builder.setNegativeButton("Cancel", null);
    builder.show();
}

function showCalendarIconPicker(iconPacks) {
    var pluginContext = context.createPackageContext(MY_PKG, 0);
    var inflater = LayoutInflater.from(pluginContext);
    var rsrc = pluginContext.getResources();
    var listItemId = rsrc.getIdentifier("two_lines_list_item", "layout", MY_PKG);
    var listItemIconId = rsrc.getIdentifier("android:id/icon", null, null);
    var listItemText1Id = rsrc.getIdentifier("android:id/text1", null, null);
    var listItemText2Id = rsrc.getIdentifier("android:id/text2", null, null);

    var adapter = new JavaAdapter(ArrayAdapter, {
        getView: function(position, convertView, parent) {
            if(convertView == null) {
                convertView = inflater.inflate(listItemId, null);
            }

            var item = iconPacks.get(position);

            var text1 = convertView.findViewById(listItemText1Id);
            var text2 = convertView.findViewById(listItemText2Id);
            var img = convertView.findViewById(listItemIconId)

            text1.setText(item.label);
            text2.setText(item.prefix);
            img.setImageDrawable(item.icon);

            return convertView;
        }
    }, context, 0, iconPacks);

    var listView = new ListView(context);
    listView.setAdapter(adapter);


    var builder=new AlertDialog.Builder(context);
    builder.setView(listView);
    builder.setTitle("Please select an icon pack");
    builder.setNegativeButton("Cancel", null);

    var dialog = builder.create();

    listView.setOnItemClickListener({
        onItemClick: function(parent, view, position, id) {
            var calendarIcon = iconPacks.get(position);
            showDayOrMonthDialog(calendarIcon);
            dialog.dismiss();
        }
    });

    dialog.show();
}

function showDayOrMonthDialog(iconPack) {
    var builder=new AlertDialog.Builder(context);
    builder.setTitle("Configuration");
    builder.setMessage("Should the icon display the month or day?");
    builder.setPositiveButton("Day", {
        onClick:function(dialog,id) {
            createItem(iconPack, false);
        }
    });
    builder.setNegativeButton("Month", {
       onClick:function(dialog,id) {
           createItem(iconPack, true);
       }
   });
   builder.show();
}

function createItem(calendarIcon, month) {
    var script_menu = installScript("week_day_menu", "Week Day / menu handler");

    var d = screen.getCurrentDesktop();

    var icon = d.addShortcut("Calendar", new Intent(), 0, 0);

    var settings = {
        dayNames: "sund,mon,tue,wed,thu,fri,sat",
        hlColor: "#ff0000"
    }

    var ed = icon.getProperties().edit();
    ed.setBoolean("i.onGrid", false);
    ed.setBoolean("s.iconVisibility", true);
    ed.setBoolean("s.labelVisibility", false);
    ed.getBox("i.box").setAlignment("CENTER", "MIDDLE");
    ed.commit();

    var now;
    var variable;
    if(month) {
        variable = "$ll_month"
        now = "Calendar.getInstance().get(Calendar.MONTH)+1";
    } else {
        variable = "$ll_month"
        now = "Calendar.getInstance().get(Calendar.DAY_OF_MONTH)";
    }
    var formula =
        "var dummy="+variable+";\n"+
        "bindClass('java.util.Calendar');\n"+
        "var now = "+now+";\n"+
        "var img = Image.createImage('"+calendarIcon.package+"', '"+calendarIcon.prefix+"'+now);\n"+
        "item.setCustomIcon(img);\n"+
        "return 0;"

    icon.setBinding("v", formula, true);

    // use the last screen touch position, if any, as location for the new item
    bindClass("java.lang.Integer");
    var x = screen.getLastTouchX();
    var y = screen.getLastTouchY();
    if(x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) {
        // no previous touch event, use a default position (can happen when using the hardware menu key for instance)
        x = d.getWidth()/2;
        y = d.getHeight()/2;
    }

    // trick: let the system compute the needed size, and center around the touch point
    icon.setSize(0, 0);
    setTimeout(function() {
        x-=icon.getWidth()/2;
        y-=icon.getHeight()/2;
        icon.setPosition(x, y);

        var ed = icon.getProperties().edit();
            ed.setBoolean("i.onGrid", true);
            ed.commit();
    }, 0);
}