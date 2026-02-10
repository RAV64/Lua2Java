package org.msuo.lua2java;

import org.luaj.vm2.LuaValue;

final class EnumAdapter implements TypeAdapter {

    private final Class<?> enumClass;

    EnumAdapter(Class<?> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public ReadResult read(
        Path path,
        LuaValue luaValue,
        ErrorCollector errors
    ) {
        if (!luaValue.isstring()) {
            errors.add(path, Errors.enumExpectedString(luaValue));
            return ReadResult.fail();
        }

        String name = luaValue.tojstring();
        try {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object e = Enum.valueOf((Class<? extends Enum>) enumClass, name);
            return ReadResult.ok(e);
        } catch (IllegalArgumentException ex) {
            errors.add(path, Errors.enumUnknown(enumClass, name));
            return ReadResult.fail();
        }
    }
}
