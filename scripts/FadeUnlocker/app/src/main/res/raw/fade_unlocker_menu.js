var mode = menu.getMode();
if(mode == Menu.MODE_ITEM_EM) {
    menu.addMainItem("Select an unlocker image", function(v) {
        var image = getActiveScreen().pickImage(0);
        item.setBoxBackground(image, "n", true);
        menu.close();
    });
}

