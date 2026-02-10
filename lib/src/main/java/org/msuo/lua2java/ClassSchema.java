package org.msuo.lua2java;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ClassSchema {

    final List<FieldBinding> bindings;

    private ClassSchema(List<FieldBinding> bindings) {
        this.bindings = bindings;
    }

    static ClassSchema build(Class<?> cls) {
        List<Field> fields = allInstanceFields(cls);
        List<FieldBinding> bs = new ArrayList<>(fields.size());

        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            f.setAccessible(true);

            String key = f.getName();
            Type t = f.getGenericType();
            TypeAdapter adapter = LuaDeserializer.adapterFor(t);

            bs.add(new FieldBinding(f, key, adapter));
        }

        return new ClassSchema(Collections.unmodifiableList(bs));
    }

    private static List<Field> allInstanceFields(Class<?> cls) {
        List<Field> out = new ArrayList<>();
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            Field[] fs = c.getDeclaredFields();
            for (int i = 0; i < fs.length; i++) {
                Field f = fs[i];
                if (Modifier.isStatic(f.getModifiers())) continue;
                out.add(f);
            }
            c = c.getSuperclass();
        }
        return out;
    }
}
