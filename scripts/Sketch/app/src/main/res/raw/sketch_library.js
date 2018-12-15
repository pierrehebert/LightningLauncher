bindClass("android.app.AlertDialog");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceListView");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceColor");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceCheckBox");
bindClass("net.pierrox.lightning_launcher.prefs.LLPreferenceSlider");

var TAG_SETTINGS = "settings";

net_pierrox_sketch = {

    showSettings: function(drawing) {
        var context = getActiveScreen().getContext();

        // extract settings stored in the container tag
        var settings = JSON.parse(drawing.getTag(TAG_SETTINGS));

        // description of our preferences
        var prefPenSize = new LLPreferenceSlider(0, "Pen size", null, settings.penSize, null, "INT", 0, 30, 1, null);

        // create a list view to display our preferences
        var listView = new LLPreferenceListView(context, null);
        listView.setPreferences([ prefPenSize ]);

        // setup the dialog and assign its content view with the preference list view
        var builder=new AlertDialog.Builder(context);
        builder.setView(listView);
        builder.setTitle("Sketch settings");
        builder.setPositiveButton("Save",{onClick:function(dialog,id){
            // update the settings and save them back in the container's tag
            settings.penSize = prefPenSize.getValue();
            alert
            drawing.setTag(TAG_SETTINGS, JSON.stringify(settings));
            dialog.dismiss();
        }});
        builder.setNegativeButton("Cancel", null);
        builder.show();
    },
     
    setCurrentColor: function(drawing, color) {
        var settings = JSON.parse(drawing.getTag(TAG_SETTINGS));
        settings.currentColor = color;
        drawing.setTag(TAG_SETTINGS, JSON.stringify(settings));
    },
    
    getCurrentColor: function(drawing) {
        var settings = JSON.parse(drawing.getTag(TAG_SETTINGS));
        return settings.currentColor;
    },

    getPenSize: function(drawing) {
        var settings = JSON.parse(drawing.getTag(TAG_SETTINGS));
        return settings.penSize;
    },

    clear: function(drawing) {
        var image = drawing.getBoxBackground("n");
        image.draw().drawARGB(255,255,255,255);
        image.update();
        image.save();
    },

    setColorButtonSelection: function(container, color, selected) {
        var btn = container.getItemByName("color_"+(color >>> 0));
        ed = btn.getProperties().edit();
        box = ed.getBox("i.box");
        box.setSize("bl,bt,br,bb",selected ? 6 : 1);
        ed.commit();
    }
};