package net.pierrox.lightning_launcher.engine.variable;

import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.script.Script;
import net.pierrox.lightning_launcher.views.item.ItemView;

import java.io.File;

public class Target {
    public ItemView itemView;

    public Variable[] variables;
    public String field;
    public Object value;
    public Script script;
    public int dateComputed = -1;

    public Target(ItemView itemView, String field, Variable[] variables, Script script) {
        this.itemView = itemView;
        this.field = field;
        this.variables = variables;
        this.script = script;
    }
}