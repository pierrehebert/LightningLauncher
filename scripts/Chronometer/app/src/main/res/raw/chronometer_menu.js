var chrono = item.my.chrono;

var mode = menu.getMode();
if(mode == Menu.MODE_ITEM_NO_EM) {
    menu.getMainItemsView().removeAllViews();
    menu.addMainItem("Reset", function(v) {
        chrono.reset();
        menu.close();
    });
    menu.addMainItem("Settings", function(v) {
        showSettings();
        menu.close();
    });
}


bindClass("android.app.AlertDialog");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceListView");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceCheckBox");

function showSettings() {
    var screen = getActiveScreen();
    var context = screen.getContext();

    var prefShowMilliSeconds = new LLPreferenceCheckBox(0, "Show milliseconds", null, chrono.getShowMilliseconds(), null);
    var listView = new LLPreferenceListView(context, null);
    listView.setPreferences([
        prefShowMilliSeconds,
    ]);

    var builder=new AlertDialog.Builder(context);
    builder.setView(listView);
    builder.setTitle("Settings");
    builder.setPositiveButton("Save",{onClick:function(dialog,id){
        chrono.setShowMilliseconds(prefShowMilliSeconds.isChecked());
        dialog.dismiss();
    }});
    builder.setNegativeButton("Cancel", null);
    builder.show();
}