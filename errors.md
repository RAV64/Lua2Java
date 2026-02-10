# errors.md

A reference for **every error message** the library can produce, with:

- a minimal **example scenario**
- what went wrong
- how to fix it

---

## How errors are reported

The deserializer throws `ConfigDeserializationException`. Each error has:

- `path` (stable JSON-style path like `$.db.port`, `$.tags[1]`, `$.limits{1}`)
- `message` (one of the messages documented below)

### Print all errors

```java
try {
  LuaDeserializer.deserialize(lua, MyCfg.class);
} catch (ConfigDeserializationException ex) {
  for (ConfigDeserializationException.ConfigError e : ex.getErrors()) {
    System.out.println(e.getPath() + " -> " + e.getMessage());
  }
}
```

---

## Missing required field

### Message

`Missing required field (no default value).`

### Description

A field had **no Java default value**, and the Lua table **did not provide the
key**.

### Java POJO

```java
public final class Cfg {
  public String host; // no default
}
```

### Lua config file

```lua
return { }
```

### What went wrong

The binder treats missing keys as “keep default if present, otherwise required”.
Since `host` is `null` by default and missing in Lua, it becomes an error.

### How to fix

- Provide the key in Lua, **or**
- Give the field a Java default value, **or**
- Make it optional with `Optional<T>` (missing ⇒ `Optional.empty()`).

---

## Primitive fields not supported

### Message

`Primitive field types are not supported: int` (and similarly for `boolean`,
`double`, etc.)

### Description

The library intentionally rejects Java primitives to stay null-safe and
explicit.

### Java POJO

```java
public final class Cfg {
  public int port;
}
```

### Lua config file

```lua
return { port = 8080 }
```

### What went wrong

The adapter refuses primitive field types.

### How to fix

Use boxed types:

- `Integer` instead of `int`
- `Boolean` instead of `boolean`
- `Double` instead of `double`

---

## Expected scalar (leaf got a table/function/etc.)

### Message

`Expected primitive (string/number/bool), got: table` (or `function`,
`userdata`, etc.)

### Description

A “leaf” field expects a scalar (string/number/bool), but Lua provided a
non-scalar value.

### Java POJO

```java
public final class Cfg {
  public String name;
}
```

### Lua config file

```lua
return { name = { nested = "nope" } }
```

### What went wrong

`String` is a leaf type; the deserializer only accepts a scalar here.

### How to fix

Change the Lua value to a scalar, or change the Java field to a nested object
type.

---

## No 1-arg constructor (custom leaf type can’t be built)

### Message

`No 1-arg constructor on <Type> accepting <ScalarType>`

### Description

Lua provided a scalar, but your custom type doesn’t have a matching 1-arg
constructor.

### Java POJO

```java
public final class Port {
  public final Integer value;

  // Wrong: no Port(Integer) constructor
  public Port(String s) { this.value = Integer.valueOf(s); }
}

public final class Cfg {
  public Port port;
}
```

### Lua config file

```lua
return { port = 8080 }
```

### What went wrong

Lua produced an `Integer` scalar, but `Port` only has `Port(String)`.

### How to fix

Add a 1-arg constructor matching the Lua scalar type you expect:

- `MyType(String)` for Lua strings
- `MyType(Integer)` for Lua integers
- `MyType(Double)` for Lua non-integer numbers

---

## Constructor rejected (validation failed)

### Message

`Value rejected by <Type> constructor: <reason>`

### Description

Your 1-arg constructor threw (commonly `IllegalArgumentException`) to reject
invalid config.

### Java POJO

```java
public final class NonEmptyString {
  public final String value;

  public NonEmptyString(String s) {
    if (s == null || s.trim().isBlank()) throw new IllegalArgumentException("must be non-empty");
    this.value = s;
  }
}

public final class Cfg {
  public NonEmptyString name;
}
```

### Lua config file

```lua
return { name = "" }
```

### What went wrong

The domain type validation rejected the scalar.

### How to fix

Provide a valid value in Lua, or relax the validation rules in the constructor.

---

## Failed calling constructor (reflection call failed)

### Message

`Failed calling constructor for <Type>: <reason>`

### Description

The library found a matching constructor but couldn’t call it successfully
(reflection error).

### Java POJO

```java
public final class Secret {
  private Secret(String s) { }
}

public final class Cfg {
  public Secret s;
}
```

### Lua config file

```lua
return { s = "x" }
```

### What went wrong

Reflection invocation can fail due to access/module rules or other reflective
constraints.

### How to fix

- Make the constructor accessible in your runtime environment, or
- Prefer non-restricted constructors for configuration/domain types.

---

## Enum expects string

### Message

`Enum expects string name, got: <typename>`

### Description

Enums are parsed from **Lua strings** by name. Any non-string value fails.

### Java POJO

```java
public enum Mode { DEV, PROD }

public final class Cfg {
  public Mode mode;
}
```

### Lua config file

```lua
return { mode = 123 }
```

### What went wrong

Enum parsing only accepts `mode = "DEV"` style values.

### How to fix

Provide the enum name as a Lua string.

---

## Unknown enum value

### Message

`Unknown enum value '<name>' for <EnumClass>`

### Description

Lua provided a string, but it doesn’t match any enum constant.

### Java POJO

```java
public enum Mode { DEV, PROD }

public final class Cfg {
  public Mode mode;
}
```

### Lua config file

```lua
return { mode = "STAGING" }
```

### What went wrong

`STAGING` is not a member of the enum.

### How to fix

Use a valid enum constant name or add it to the enum.

---

## Map expected table

### Message

`Expected table for Map, got: <typename>`

### Description

`Map<K,V>` fields require a Lua table.

### Java POJO

```java
public final class Cfg {
  public java.util.Map<String, Integer> limits;
}
```

### Lua config file

```lua
return { limits = 123 }
```

### What went wrong

A map must be bound from a Lua table (`{ key = value }`).

### How to fix

Make `limits` a Lua table.

---

## Collection expected table/array (List/Set)

### Message

`Expected table/array for <CollectionType>, got: <typename>`

### Description

`List<T>`, `Set<T>`, and `Collection<T>` fields require a Lua table
(array-like).

### Java POJO

```java
public final class Cfg {
  public java.util.List<String> tags;
}
```

### Lua config file

```lua
return { tags = "a,b,c" }
```

### What went wrong

Collections are bound from Lua arrays like `{ "a", "b" }`.

### How to fix

Use a Lua array table for the collection.

---

## Optional inner must be concrete (no nested generics)

### Message

`Optional inner type must be a concrete class (no nested generics). Got: ...`

### Description

`Optional<T>` only supports `T` as a **concrete class** (no nested generics /
wildcards).

### Java POJO

```java
public final class Cfg {
  public java.util.Optional<java.util.List<String>> tags;
}
```

### Lua config file

```lua
return { tags = { "a", "b" } }
```

### What went wrong

The inner type is `List<String>` (a parameterized type), not a concrete
`Class<?>`.

### How to fix

Make the optional point to a concrete class:

- Wrap the list in a POJO:

  - `Optional<TagsCfg>` where `TagsCfg` has `List<String> tags;`
- Or avoid optional collection and instead use a default:

  - `List<String> tags = List.of();`

---

## Collection element must be concrete (no nested generics)

### Message

`Collection element type must be a concrete class (no nested generics). Got: ...`

### Description

`List<T>` / `Set<T>` require `T` to be a concrete class.

### Java POJO

```java
public final class Cfg {
  public java.util.List<java.util.Optional<String>> tags;
}
```

### Lua config file

```lua
return { tags = { "a", "b" } }
```

### What went wrong

Element type is `Optional<String>` (parameterized), not a concrete class.

### How to fix

Use a concrete element type. If you need optionality per element, encode it
explicitly:

- Use sentinel values (e.g. empty string) with a validated type, or
- Wrap elements in a small POJO with defaults.

---

## Map key must be concrete (no nested generics)

### Message

`Map key type must be a concrete class (no nested generics). Got: ...`

### Description

`Map<K,V>` requires `K` to be a concrete class.

### Java POJO

```java
public final class Cfg {
  public java.util.Map<java.util.Optional<String>, Integer> bad;
}
```

### Lua config file

```lua
return { bad = { foo = 1 } }
```

### What went wrong

Key type is parameterized (`Optional<String>`), not a concrete class.

### How to fix

Use a concrete key type like `String` or a domain type with a 1-arg constructor.

---

## Map value must be concrete (no nested generics)

### Message

`Map value type must be a concrete class (no nested generics). Got: ...`

### Description

`Map<K,V>` requires `V` to be a concrete class.

### Java POJO

```java
public final class Cfg {
  public java.util.Map<String, java.util.List<String>> bad;
}
```

### Lua config file

```lua
return { bad = { foo = { "a" } } }
```

### What went wrong

Value type is `List<String>` (parameterized), not a concrete class.

### How to fix

Wrap the value in a concrete POJO:

```java
public final class Tags {
  public java.util.List<String> value;
}

public final class Cfg {
  public java.util.Map<String, Tags> good;
}
```

---

## No no-arg constructor (nested object can’t be instantiated)

### Message

`No no-arg constructor for nested object type: <Class>`

### Description

Nested object binding instantiates the object with a no-arg constructor, then
sets fields.

### Java POJO

```java
public final class DbCfg {
  public String host;

  public DbCfg(String host) { this.host = host; } // no no-arg ctor
}

public final class Cfg {
  public DbCfg db;
}
```

### Lua config file

```lua
return { db = { host = "localhost" } }
```

### What went wrong

The library can’t create `DbCfg` without a no-arg constructor.

### How to fix

Add a no-arg constructor (can be private if reflection access is allowed), or
provide defaults and setters/fields.

---

## Constructor failed (no-arg constructor threw)

### Message

`Constructor failed for <Class>: <cause message>`

### Description

The no-arg constructor exists, but it threw an exception.

### Java POJO

```java
public final class DbCfg {
  public String host;

  public DbCfg() {
    throw new IllegalStateException("boom");
  }
}

public final class Cfg {
  public DbCfg db;
}
```

### Lua config file

```lua
return { db = { host = "localhost" } }
```

### What went wrong

Object instantiation invoked the no-arg constructor, which threw.

### How to fix

Make the no-arg constructor safe (don’t throw), and move validation to
leaf/domain constructors or post-load checks.

---

## Failed to instantiate (reflection instantiation error)

### Message

`Failed to instantiate <Class>: <reason>`

### Description

Instantiation failed for reasons other than “no no-arg ctor” or “constructor
threw” (e.g., abstract class, interface).

### Java POJO

```java
public interface DbCfg {
  String getHost();
}

public final class Cfg {
  public DbCfg db;
}
```

### Lua config file

```lua
return { db = { host = "localhost" } }
```

### What went wrong

Interfaces/abstract types can’t be instantiated.

### How to fix

Use a concrete class with a no-arg constructor.

---

## Unsupported Type (reflection type isn’t Class/ParameterizedType)

### Message

`Unsupported Type: <Type>`

### Description

Some Java type signatures reflect to `Type` kinds that are not supported (like
generic arrays or type variables).

### Java POJO

```java
public final class Cfg {
  public java.util.List<String>[] tags; // generic array type
}
```

### Lua config file

```lua
return { tags = { { "a" }, { "b" } } }
```

### What went wrong

The field’s reflective type isn’t a plain `Class<?>` or a supported
`ParameterizedType`.

### How to fix

Use supported shapes:

- `List<String>` instead of `List<String>[]`
- Avoid fields declared as type variables (e.g. `T value` on a generic class
  used as a config root)

---

## Unsupported parameterized type (generic raw type not recognized)

### Message

`Unsupported parameterized type: <Type>`

### Description

Only these parameterized forms are supported:

- `Optional<T>`
- `Map<K,V>`
- `Collection<T>` (including `List<T>`, `Set<T>`)

Other parameterized types are rejected.

### Java POJO

```java
public final class Cfg {
  public java.util.concurrent.atomic.AtomicReference<String> ref;
}
```

### Lua config file

```lua
return { ref = "x" }
```

### What went wrong

`AtomicReference<T>` is parameterized, but not one of the supported generic
containers.

### How to fix

Use supported containers or wrap it in a POJO and deserialize into a supported
shape first.

---

## Unsupported parameterized raw type (rare)

### Message

`Unsupported parameterized raw type: <Type>`

### Description

The deserializer expects `pt.getRawType()` to be a `Class<?>`. In normal Java
source code this is almost always true; this error tends to appear with unusual
bytecode-generated types.

### Java POJO

```java
// This is hard to produce in normal Java source;
// typically comes from unusual generated signatures.
public final class Cfg {
  public Object something;
}
```

### Lua config file

```lua
return { something = "x" }
```

### What went wrong

A parameterized type was encountered whose raw type wasn’t a `Class<?>`.

### How to fix

Change the field type to a normal declared class/parameterization
(`Optional<T>`, `List<T>`, `Map<K,V>`).

---

## Failed to set field (access)

### Message

`Failed to set field (access): <reason>`

### Description

The deserializer instantiated an object and parsed a value, but Java reflection
couldn’t assign it to the field.

### Java POJO

```java
public final class Cfg {
  public final Integer port = 80; // final field
}
```

### Lua config file

```lua
return { port = 8080 }
```

### What went wrong

Setting certain fields (especially `final`) may be blocked by the JVM/runtime
rules.

### How to fix

- Don’t make deserialized fields `final`, or
- Treat them as true constants and don’t provide them from Lua.

---

## Failed to set field (type mismatch)

### Message

`Failed to set field (type mismatch): <reason>`

### Description

The library produced a value of a different concrete type than the field
requires.

This most commonly happens when you declare **concrete collection/map
implementations** instead of the **interface types**, because the library
returns:

- `ArrayList` for list-like collections
- `LinkedHashSet` for set-like collections
- `LinkedHashMap` for maps

### Java POJO

```java
public final class Cfg {
  public java.util.HashSet<String> tags; // concrete type
}
```

### Lua config file

```lua
return { tags = { "a", "b" } }
```

### What went wrong

The adapter returns a `LinkedHashSet`, which is not assignable to `HashSet`.

### How to fix

Declare interface types:

```java
public final class Cfg {
  public java.util.Set<String> tags;
  public java.util.List<String> list;
  public java.util.Map<String, Integer> map;
}
```

---

## Appendix: Common fixes checklist

- Use boxed types: `Integer`, `Double`, `Boolean` (not primitives).
- For nested objects: ensure a **no-arg constructor** exists and doesn’t throw.
- For validated/domain leaf types: add **1-arg constructors** for the scalar
  types you expect (`String`, `Integer`, `Double`).
- Use container interfaces (`List`, `Set`, `Map`) rather than concrete
  implementations (`ArrayList`, `HashSet`, `HashMap`).
- Keep generics “flat” in supported containers:

  - ✅ `Optional<MyPojo>`, `List<MyPojo>`, `Map<String, MyPojo>`
  - ❌ `Optional<List<String>>`, `Map<String, List<String>>`,
    `List<Optional<String>>`
