package net.pierrox.lightning_launcher.engine.variable;

import android.graphics.Color;

public class Value {
    public static boolean asBoolean(Object value) {
        if(value == null) return false;
        Class<?> cls = value.getClass();
        if(cls == Boolean.class) {
            return (boolean) value;
        } else if(cls == Integer.class) {
            return (int)value != 0;
        } else if(cls == Long.class) {
            return (long)value != 0;
        } else if(cls == Float.class) {
            return (float) value != 0;
        } else if(cls == Double.class) {
            return (double) value != 0;
        } else if (cls == String.class) {
            return "true".equals(value);
        } else {
            return false;
        }
    }

    public static int asInteger(Object value) {
        if(value == null) return 0;
        Class<?> cls = value.getClass();
        if(cls == Boolean.class) {
            return (boolean) value ? 1 : 0;
        } else if(cls == Integer.class) {
            return (int)value;
        } else if(cls == Long.class) {
            return ((Long)value).intValue();
        } else if(cls == Float.class) {
            return Math.round((float) value);
        } else if(cls == Double.class) {
            return (int)Math.round((double) value);
        } else if (cls == String.class) {
            return parseIntOrColor((String) value);
        } else {
            return 0;
        }
    }

    public static float asFloat(Object value) {
        if(value == null) return 0;
        Class<?> cls = value.getClass();
        if(cls == Boolean.class) {
            return (boolean) value ? 1 : 0;
        } else if(cls == Integer.class) {
            return (int)value;
        } else if(cls == Long.class) {
            return ((Long)value).floatValue();
        } else if(cls == Float.class) {
            return (float) value;
        } else if(cls == Double.class) {
            return ((Double)value).floatValue();
        } else if (cls == String.class) {
            try {
                return Float.parseFloat((String)value);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static double asDouble(Object value) {
        if(value == null) return 0;
        Class<?> cls = value.getClass();
        if(cls == Boolean.class) {
            return (boolean) value ? 1 : 0;
        } else if(cls == Integer.class) {
            return (int)value;
        } else if(cls == Long.class) {
            return ((Long)value).doubleValue();
        } else if(cls == Float.class) {
            return (float) value;
        } else if(cls == Double.class) {
            return (Double)value;
        } else if (cls == String.class) {
            try {
                return Double.parseDouble((String)value);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static String asString(Object value) {
        if(value == null) return null;
        Class<?> cls = value.getClass();
        if(cls == Boolean.class) {
            return (boolean) value ? "true" : "false";
        } else if (cls == String.class) {
            return (String) value;
        } else {
            return value.toString();
        }
    }

    public static int parseIntOrColor(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            if(s.startsWith("0x")) {
                s = "#"+s.substring(2);
            }
            try {
                return Color.parseColor(s);
            } catch(IllegalArgumentException e2) {
                return 0;
            }
        }
    }
}
