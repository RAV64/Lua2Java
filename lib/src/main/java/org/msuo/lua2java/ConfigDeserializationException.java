package org.msuo.lua2java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigDeserializationException extends RuntimeException {

    private final List<ConfigError> errors;

    public ConfigDeserializationException(List<ConfigError> errors) {
        super(buildMessage(errors));
        this.errors = Collections.unmodifiableList(
            new ArrayList<ConfigError>(errors)
        );
    }

    public List<ConfigError> getErrors() {
        return errors;
    }

    private static String buildMessage(List<ConfigError> errors) {
        StringBuilder sb = new StringBuilder(
            "Config deserialization failed:\n"
        );
        for (ConfigError e : errors) {
            sb
                .append(" - ")
                .append(e.getPath())
                .append(": ")
                .append(e.getMessage())
                .append("\n");
        }
        return sb.toString();
    }

    public static final class ConfigError {

        private final String path;
        private final String message;

        public ConfigError(String path, String message) {
            this.path = path;
            this.message = message;
        }

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
        }
    }
}
