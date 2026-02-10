package org.msuo.lua2java;

import java.util.ArrayList;
import java.util.List;

final class ErrorCollector {

    private final List<ConfigDeserializationException.ConfigError> errors =
        new ArrayList<>();

    void add(Path path, String msg) {
        errors.add(
            new ConfigDeserializationException.ConfigError(path.toString(), msg)
        );
    }

    boolean hasErrors() {
        return !errors.isEmpty();
    }

    List<ConfigDeserializationException.ConfigError> asList() {
        return errors;
    }
}
