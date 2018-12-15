function Container(name, id, children) {
    this.name = name;
    this.id = id;
    this.children = children;
}

function check(item, array) {
    var t = item.getType();
    if (t == "Panel" || t == "Folder") {
        var c = item.getContainer();
        var name = t + (t == "Folder" ? " (" + item.getLabel() + ")" : "") + " #" + c.getId().toString(16);
        var children = [];
        var items = c.getAllItems();
        for (var i = 0; i < items.length; i++) {
            check(items[i], children);
        }
        array.push(new Container(name, c.getId(), children));
    }
}

var result = [];
var desktops = LL.getAllDesktops();
for (var i = 0; i < desktops.getLength(); i++) {
    var d = LL.getContainerById(desktops.getAt(i));
    var children = [];
    var items = d.getAllItems();
    for (var j = 0; j < items.length; j++) {
        check(items[j], children);
    }
    result.push(new Container("Desktop (" + d.getName() + ") #" + d.getId().toString(16), d.getId(), children));
}
return JSON.stringify(result);