package org.msuo.lua2java;

import org.luaj.vm2.LuaValue;

final class LuaScalar {

    final Object value;
    final Class<?> boxedType;

    private LuaScalar(Object value, Class<?> boxedType) {
        this.value = value;
        this.boxedType = boxedType;
    }

    static LuaScalar from(LuaValue luaValue) {
        switch (luaValue.type()) {
            case LuaValue.TSTRING:
                return new LuaScalar(luaValue.tojstring(), String.class);
            case LuaValue.TNUMBER:
                if (luaValue.isint()) {
                    return new LuaScalar(
                        Integer.valueOf(luaValue.toint()),
                        Integer.class
                    );
                }
                return new LuaScalar(
                    Double.valueOf(luaValue.todouble()),
                    Double.class
                );
            case LuaValue.TBOOLEAN:
                return new LuaScalar(
                    Boolean.valueOf(luaValue.toboolean()),
                    Boolean.class
                );
            default:
                return null;
        }
    }
}
