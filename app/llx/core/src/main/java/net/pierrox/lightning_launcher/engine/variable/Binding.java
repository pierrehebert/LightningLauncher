package net.pierrox.lightning_launcher.engine.variable;

public final class Binding {
    public boolean enabled;
    public String target;
    public String formula;

    public Binding(String target, String formula, boolean enabled) {
        this.target = target;
        this.formula = formula;
        this.enabled = enabled;
    }

    public boolean equals(Binding other) {
        if(target == null && other.target!=null || (target != null && !target.equals(other.target))) return false;
        if(formula == null && other.formula!=null || (formula != null && !formula.equals(other.formula))) return false;
        return true;
    }
}
