package com.google.android.vending.licensing.util;

public class Item {
    private String name;
    private String value;

    public Item(String n, String v) {
        name = n;
        value = v;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}