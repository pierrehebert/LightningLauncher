package net.pierrox.lightning_launcher.engine.variable;

public class Variable {
    public String name;
    public Object value;

    public Variable(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return name;
    }

    public String describe() {
        return "Set "+name+" to "+value;
    }

    public static Variable decode(String data) {
        if(data == null) return null;
        int pos = data.indexOf('/');
        if(pos == -1) {
            return null;
        }
        String name = data.substring(0, pos);
        String value = data.substring(pos+1);
        return new Variable(name, value);
    }

    public String encode() {
        return name+"/"+value;
    }
}
