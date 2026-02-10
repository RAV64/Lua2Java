package org.msuo.lua2java;

final class Path {

    private final String value;

    private Path(String value) {
        this.value = value;
    }

    static Path root() {
        return new Path("$");
    }

    Path field(String name) {
        return new Path(value + "." + name);
    }

    Path index(int i) {
        return new Path(value + "[" + i + "]");
    }

    Path mapKey(Object key) {
        return new Path(value + "[" + String.valueOf(key) + "]");
    }

    Path rawKey(Object rawKey) {
        return new Path(value + "{" + String.valueOf(rawKey) + "}");
    }

    @Override
    public String toString() {
        return value;
    }
}
