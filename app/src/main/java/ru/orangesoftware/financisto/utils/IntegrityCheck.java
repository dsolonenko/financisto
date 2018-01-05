package ru.orangesoftware.financisto.utils;

public interface IntegrityCheck {

    enum Level {
        OK, INFO, WARN, ERROR
    }

    class Result {
        public static final Result OK = new Result(Level.OK, "");

        public final Level level;
        public final String message;

        Result(Level level, String message) {
            this.level = level;
            this.message = message;
        }
    }


    Result check();

}
