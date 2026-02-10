package org.msuo.lua2java;

import java.lang.reflect.Field;

final class FieldBinding {

    final Field field;
    final String key;
    final TypeAdapter adapter;

    FieldBinding(Field field, String key, TypeAdapter adapter) {
        this.field = field;
        this.key = key;
        this.adapter = adapter;
    }
}
