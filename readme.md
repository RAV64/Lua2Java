# Lua2Java Deserializer

A tiny, **null-safe** configuration library for **deserializing Lua tables into
Java objects** with **constructor validation** baked in.

Write your config in Lua (`return { ... }`), map it to simple Java POJOs, and
let the deserializer:

- nested lua tables -> nested java objects
- handle `Optional<T>` cleanly
- null-safe
- keep Java-side **default field values** when keys are missing
- support `List`, `Set`, `Map`
- support enums from strings

---

## Usage

```java
// From a Lua string (already loaded into `lua`)
MyConfig cfg = LuaDeserializer.deserialize(lua, MyConfig.class);
```

```java
// From a file
MyConfig cfg = LuaDeserializer.deserialize(new java.io.File("config.lua"), MyConfig.class);
```

---

## Quick real-world example

A small app config that shows **nested objects**, **enum**, **Optional**, and
**Java defaults** (no validation/domain types yet).

### Description

- `app` is a nested object
- `mode` is an enum parsed from a Lua string
- `user` is optional (missing key ⇒ `Optional.empty()`)
- `timeoutMs` has a Java default kept when Lua omits the key

### Java POJO

```java
public enum Mode { DEV, PROD }

public final class App {
  public String host;
  public Integer port;
  public Mode mode;

  public java.util.Optional<String> user;     // missing => Optional.empty()
  public Integer timeoutMs = 5000;            // missing => keep default
}

public final class Config {
  public App app;
}
```

### Lua config file

```lua
-- config.lua
return {
  app = {
    host = "localhost",
    port = 8080,
    mode = "PROD"
    -- user omitted (Optional)
    -- timeoutMs omitted (Java default)
  }
}
```

### Assertions

```java
Config cfg = LuaDeserializer.deserialize(lua, Config.class);

assertEquals("localhost", cfg.app.host);
assertEquals(Integer.valueOf(8080), cfg.app.port);
assertEquals(Mode.PROD, cfg.app.mode);

assertEquals(java.util.Optional.empty(), cfg.app.user);
assertEquals(Integer.valueOf(5000), cfg.app.timeoutMs);
```

---

## Validation

Prefer **domain types** that enforce invariants in their constructors. The
deserializer will:

- use scalar assignment if the field type matches (`String`, `Integer`,
  `Double`, `Boolean`)
- otherwise, try a **1-arg constructor** whose parameter type matches the Lua
  scalar type

### Description

- `NonEmptyString(String)` validates string fields
- `PositiveInteger(Integer)` validates integer fields
- `PositiveDouble(Double)` validates floating-point fields
- Constructors can accept **String**, **Integer**, or **Double** (and you can
  define multiple types as needed)

### Java POJO

```java
public final class NonEmptyString {
  public final String value;

  public NonEmptyString(String s) {
    if (s == null || s.trim().isBlank()) throw new IllegalArgumentException("must be non-empty");
    this.value = s;
  }

  @Override public String toString() { return value; }
}

public final class PositiveInteger {
  public final Integer value;

  public PositiveInteger(Integer i) {
    if (i == null || i <= 0) throw new IllegalArgumentException("must be > 0");
    this.value = i;
  }
}

public final class PositiveDouble {
  public final Double value;

  public PositiveDouble(Double d) {
    if (d == null || d <= 0.0) throw new IllegalArgumentException("must be > 0");
    this.value = d;
  }
}

public final class ValidatedCfg {
  public NonEmptyString serviceName;   // constructed from Lua string
  public PositiveInteger retries;      // constructed from Lua integer
  public PositiveDouble backoff;       // constructed from Lua double
}
```

### Lua config file

```lua
return {
  serviceName = "worker-a",
  retries = 3,
  backoff = 1.25
}
```

### Assertions

```java
ValidatedCfg cfg = LuaDeserializer.deserialize(lua, ValidatedCfg.class);

assertEquals("worker-a", cfg.serviceName.value);
assertEquals(Integer.valueOf(3), cfg.retries.value);
assertEquals(Double.valueOf(1.25), cfg.backoff.value);
```

---

## Environment

Because configs are Lua, you can compute values, branch, and adapt at load time.

### Description

- Uses `os.getenv("DEVELOPMENT")` to pick dev/prod values

### Java POJO

```java
public enum Mode { DEV, PROD }

public final class EnvCfg {
  public Mode mode;
  public String logLevel;
  public Integer port;
}
```

### Lua config file

```lua
local cfg = {
  mode = "PROD",
  logLevel = "INFO",
  port = 80
}

if os.getenv("DEVELOPMENT") == "1" then
  cfg.mode = "DEV"
  cfg.logLevel = "DEBUG"
  cfg.port = 8080
end

return cfg
```

### Assertions

```java
EnvCfg cfg = LuaDeserializer.deserialize(lua, EnvCfg.class);

if ("1".equals(System.getenv("DEVELOPMENT"))) {
  assertEquals(Mode.DEV, cfg.mode);
  assertEquals("DEBUG", cfg.logLevel);
  assertEquals(Integer.valueOf(8080), cfg.port);
} else {
  assertEquals(Mode.PROD, cfg.mode);
  assertEquals("INFO", cfg.logLevel);
  assertEquals(Integer.valueOf(80), cfg.port);
}
```

---

## Enum

### Description

- Enums are parsed from Lua strings by name

### Java POJO

```java
public enum Mode { DEV, PROD }

public final class EnumCfg {
  public Mode mode;
}
```

### Lua config file

```lua
return { mode = "DEV" }
```

### Assertions

```java
EnumCfg cfg = LuaDeserializer.deserialize(lua, EnumCfg.class);
assertEquals(Mode.DEV, cfg.mode);
```

---

## Optional

### Description

- Missing key ⇒ `Optional.empty()`
- Provided key ⇒ parsed and wrapped in `Optional.of(...)`

### Java POJO

```java
public final class OptionalCfg {
  public java.util.Optional<String> user;
}
```

### Lua config file

```lua
return { user = "alice" }
```

### Assertions

```java
OptionalCfg cfg = LuaDeserializer.deserialize(lua, OptionalCfg.class);

assertTrue(cfg.user.isPresent());
assertEquals("alice", cfg.user.get());
```

---

## Default values

### Description

- If a Lua key is **missing**, the deserializer keeps the current Java field
  value (your default)

### Java POJO

```java
public final class DefaultsCfg {
  public String host = "localhost";
  public Integer timeoutMs = 5000;
}
```

### Lua config file

```lua
return {
  host = "example.internal"
  -- timeoutMs omitted => keep 5000
}
```

### Assertions

```java
DefaultsCfg cfg = LuaDeserializer.deserialize(lua, DefaultsCfg.class);

assertEquals("example.internal", cfg.host);
assertEquals(Integer.valueOf(5000), cfg.timeoutMs);
```

---

## Nested objects

### Description

- Nested Lua tables bind to nested Java objects

### Java POJO

```java
public final class DbCfg {
  public String host;
  public Integer port;
}

public final class NestedCfg {
  public DbCfg db;
}
```

### Lua config file

```lua
return {
  db = {
    host = "db.internal",
    port = 5432
  }
}
```

### Assertions

```java
NestedCfg cfg = LuaDeserializer.deserialize(lua, NestedCfg.class);

assertEquals("db.internal", cfg.db.host);
assertEquals(Integer.valueOf(5432), cfg.db.port);
```

---

## List

### Description

- Lua arrays (`{ 'a', 'b' }`) bind to `List<T>`

### Java POJO

```java
public final class ListCfg {
  public java.util.List<String> tags;
}
```

### Lua config file

```lua
return { tags = { "a", "b", "c" } }
```

### Assertions

```java
ListCfg cfg = LuaDeserializer.deserialize(lua, ListCfg.class);

assertEquals(3, cfg.tags.size());
assertEquals("a", cfg.tags.get(0));
assertEquals("c", cfg.tags.get(2));
```

---

## Set

### Description

- Lua arrays bind to `Set<T>` (deduplicates naturally)

### Java POJO

```java
public final class SetCfg {
  public java.util.Set<String> tags;
}
```

### Lua config file

```lua
return { tags = { "a", "b", "a" } }
```

### Assertions

```java
SetCfg cfg = LuaDeserializer.deserialize(lua, SetCfg.class);

assertEquals(2, cfg.tags.size());
assertTrue(cfg.tags.contains("a"));
assertTrue(cfg.tags.contains("b"));
```

---

## Map

### Description

- Lua tables with key/value pairs bind to `Map<K, V>`
- Keys and values can be scalars (`String`, `Integer`, `Double`, `Boolean`) or
  your domain types via 1-arg constructors

### Java POJO

```java
public final class MapCfg {
  public java.util.Map<String, Integer> limits;
}
```

### Lua config file

```lua
return {
  limits = {
    foo = 1,
    bar = 2
  }
}
```

### Assertions

```java
MapCfg cfg = LuaDeserializer.deserialize(lua, MapCfg.class);

assertEquals(Integer.valueOf(1), cfg.limits.get("foo"));
assertEquals(Integer.valueOf(2), cfg.limits.get("bar"));
```

---

## Map keys as domain types

### Description

- Map keys can also be validated/domain objects (constructed from the Lua key)

### Java POJO

```java
public final class NonEmptyString {
  public final String value;

  public NonEmptyString(String s) {
    if (s == null || s.trim().isBlank()) throw new IllegalArgumentException("must be non-empty");
    this.value = s;
  }

  @Override public String toString() { return value; }
  @Override public boolean equals(Object o) { return (o instanceof NonEmptyString) && value.equals(((NonEmptyString)o).value); }
  @Override public int hashCode() { return value.hashCode(); }
}

public final class PositiveInteger {
  public final Integer value;

  public PositiveInteger(Integer i) {
    if (i == null || i <= 0) throw new IllegalArgumentException("must be > 0");
    this.value = i;
  }

  @Override public boolean equals(Object o) {
    if (o instanceof PositiveInteger) return value.equals(((PositiveInteger)o).value);
    if (o instanceof Integer) return value.equals(o);
    return false;
  }

  @Override public int hashCode() { return java.util.Objects.hashCode(value); }
}

public final class DomainMapCfg {
  public java.util.Map<NonEmptyString, PositiveInteger> limits;
}
```

### Lua config file

```lua
return {
  limits = {
    foo = 10,
    bar = 20
  }
}
```

### Assertions

```java
DomainMapCfg cfg = LuaDeserializer.deserialize(lua, DomainMapCfg.class);

assertEquals(new PositiveInteger(10), cfg.limits.get(new NonEmptyString("foo")));
assertEquals(new PositiveInteger(20), cfg.limits.get(new NonEmptyString("bar")));
```

---

## List of complex objects

### Description

- Collections can contain nested objects (tables inside arrays)

### Java POJO

```java
public final class Item {
  public String name;
  public Integer n;
}

public final class ItemsCfg {
  public java.util.List<Item> items;
}
```

### Lua config file

```lua
return {
  items = {
    { name = "a", n = 1 },
    { name = "b", n = 2 }
  }
}
```

### Assertions

```java
ItemsCfg cfg = LuaDeserializer.deserialize(lua, ItemsCfg.class);

assertEquals(2, cfg.items.size());
assertEquals("a", cfg.items.get(0).name);
assertEquals(Integer.valueOf(2), cfg.items.get(1).n);
```

---

## Map of complex objects

### Description

- Maps can contain nested objects (tables as values)

### Java POJO

```java
public final class Item {
  public Integer n;
}

public final class ItemsByKeyCfg {
  public java.util.Map<String, Item> items;
}
```

### Lua config file

```lua
return {
  items = {
    x = { n = 1 },
    y = { n = 2 }
  }
}
```

### Assertions

```java
ItemsByKeyCfg cfg = LuaDeserializer.deserialize(lua, ItemsByKeyCfg.class);

assertEquals(Integer.valueOf(1), cfg.items.get("x").n);
assertEquals(Integer.valueOf(2), cfg.items.get("y").n);
```

---

## Scalar behavior (String / Integer / Double / Boolean)

### Description

- Lua strings -> `String`
- Lua booleans -> `Boolean`
- Lua numbers:

  - integers -> `Integer`
  - non-integers -> `Double`

### Java POJO

```java
public final class ScalarsCfg {
  public String s;
  public Integer i;
  public Double d;
  public Boolean b;
}
```

### Lua config file

```lua
return {
  s = "hello",
  i = 3,
  d = 1.25,
  b = true
}
```

### Assertions

```java
ScalarsCfg cfg = LuaDeserializer.deserialize(lua, ScalarsCfg.class);

assertEquals("hello", cfg.s);
assertEquals(Integer.valueOf(3), cfg.i);
assertEquals(Double.valueOf(1.25), cfg.d);
assertEquals(Boolean.TRUE, cfg.b);
```

---

## Note

This project is a **work in progress**. **LLM assistance was used** while
developing parts of the code and documentation.

> **Looking for error messages and fixes?**\
> See **[`errors.md`](./errors.md)** (in the project root) for a complete
> catalog of all possible deserialization errors, what they mean, and how to
> resolve them.
