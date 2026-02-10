package org.msuo.lua2java;

final class ReadResult {

    final boolean ok;
    final Object value;

    private static final ReadResult FAIL = new ReadResult(false, null);

    private ReadResult(boolean ok, Object value) {
        this.ok = ok;
        this.value = value;
    }

    static ReadResult ok(Object value) {
        return new ReadResult(true, value);
    }

    static ReadResult fail() {
        return FAIL;
    }
}
