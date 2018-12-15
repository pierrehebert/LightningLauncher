package net.pierrox.lightning_launcher.script.api;

/**
 * A binding links a "target" (a property) with a "formula" (a value).
 */
public class Binding {
    private boolean enabled;
    private String target;
    private String formula;

    public Binding(String target, String formula, boolean enabled) {
        this.target = target;
        this.formula = formula;
        this.enabled = enabled;
    }

    /**
     * @hide
     */
    public Binding(net.pierrox.lightning_launcher.engine.variable.Binding binding) {
        this(binding.target, binding.formula, binding.enabled);
    }

    public String getTarget() {
        return target;
    }

    public String getFormula() {
        return formula;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "target: "+target+", formula: "+formula+(enabled ? " (enabled)":" (disabled)");
    }
}
