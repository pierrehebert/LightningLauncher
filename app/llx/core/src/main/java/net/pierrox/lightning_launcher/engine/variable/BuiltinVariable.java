package net.pierrox.lightning_launcher.engine.variable;

public class BuiltinVariable {
    public String name;
    public String label;

    public BuiltinVariable(String name, String label) {
        this.name = name;
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
