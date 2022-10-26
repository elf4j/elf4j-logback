/*
 * MIT License
 *
 * Copyright (c) 2022 Easy Logging Facade for Java (ELF4J)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package elf4j.logback;

import elf4j.Level;
import elf4j.Logger;
import elf4j.util.NoopLogger;
import lombok.NonNull;
import lombok.ToString;
import net.jcip.annotations.Immutable;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static elf4j.Level.*;

@Immutable
@ToString
class LogbackLogger implements Logger {
    private static final Level DEFAULT_LEVEL = INFO;
    private static final String EMPTY_MESSAGE = "";
    private static final String FQCN = LogbackLogger.class.getName();
    private static final EnumMap<Level, Integer> LEVEL_MAP = setLeveMap();
    private static final EnumMap<Level, Map<String, LogbackLogger>> LOGGER_CACHE = initLoggerCache();
    @NonNull private final String name;
    @NonNull private final Level level;
    @NonNull private final ch.qos.logback.classic.Logger nativeLogger;

    private LogbackLogger(@NonNull String name, @NonNull Level level) {
        this.name = name;
        this.level = level;
        this.nativeLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
    }

    static LogbackLogger instance() {
        return getLogger(CallingStackUtil.getElf4jLoggerClientClassName());
    }

    static LogbackLogger instance(String name) {
        return getLogger(name == null ? CallingStackUtil.getElf4jLoggerClientClassName() : name);
    }

    static LogbackLogger instance(Class<?> clazz) {
        return getLogger(clazz == null ? CallingStackUtil.getElf4jLoggerClientClassName() : clazz.getName());
    }

    private static LogbackLogger getLogger(@NonNull String name, @NonNull Level level) {
        return LOGGER_CACHE.get(level).computeIfAbsent(name, k -> new LogbackLogger(k, level));
    }

    private static LogbackLogger getLogger(String name) {
        return getLogger(name, DEFAULT_LEVEL);
    }

    private static EnumMap<Level, Map<String, LogbackLogger>> initLoggerCache() {
        EnumMap<Level, Map<String, LogbackLogger>> loggerCache = new EnumMap<>(Level.class);
        EnumSet.allOf(Level.class).forEach(level -> loggerCache.put(level, new ConcurrentHashMap<>()));
        return loggerCache;
    }

    private static EnumMap<Level, Integer> setLeveMap() {
        EnumMap<Level, Integer> levelMap = new EnumMap<>(Level.class);
        levelMap.put(TRACE, LocationAwareLogger.TRACE_INT);
        levelMap.put(DEBUG, LocationAwareLogger.DEBUG_INT);
        levelMap.put(INFO, LocationAwareLogger.INFO_INT);
        levelMap.put(WARN, LocationAwareLogger.WARN_INT);
        levelMap.put(ERROR, LocationAwareLogger.ERROR_INT);
        return levelMap;
    }

    @Override
    public @NonNull String getName() {
        return this.name;
    }

    @Override
    public @NonNull Level getLevel() {
        return this.level;
    }

    @Override
    public Logger atTrace() {
        return atLevel(TRACE);
    }

    @Override
    public Logger atDebug() {
        return atLevel(DEBUG);
    }

    @Override
    public Logger atInfo() {
        return atLevel(INFO);
    }

    @Override
    public Logger atWarn() {
        return atLevel(WARN);
    }

    @Override
    public Logger atError() {
        return atLevel(ERROR);
    }

    @Override
    public void log(Object message) {
        if (isLevelDisabled()) {
            return;
        }
        nativeLogger.log(null, FQCN, LEVEL_MAP.get(this.level), Objects.toString(message), null, null);
    }

    @Override
    public void log(Supplier<?> message) {
        if (isLevelDisabled()) {
            return;
        }
        nativeLogger.log(null, FQCN, LEVEL_MAP.get(this.level), Objects.toString(message.get()), null, null);
    }

    @Override
    public void log(String message, Object... args) {
        nativeLogger.log(null, FQCN, LEVEL_MAP.get(this.level), message, args, null);
    }

    @Override
    public void log(String message, Supplier<?>... args) {
        if (isLevelDisabled()) {
            return;
        }
        nativeLogger.log(null,
                FQCN,
                LEVEL_MAP.get(this.level),
                message,
                Arrays.stream(args).map(Supplier::get).toArray(Object[]::new),
                null);
    }

    @Override
    public void log(Throwable t) {
        nativeLogger.log(null, FQCN, LEVEL_MAP.get(this.level), EMPTY_MESSAGE, null, t);
    }

    @Override
    public void log(Throwable t, Object message) {
        if (isLevelDisabled()) {
            return;
        }
        nativeLogger.log(null, FQCN, LEVEL_MAP.get(this.level), Objects.toString(message), null, t);
    }

    @Override
    public void log(Throwable t, Supplier<?> message) {
        if (isLevelDisabled()) {
            return;
        }
        nativeLogger.log(null, FQCN, LEVEL_MAP.get(this.level), Objects.toString(message.get()), null, t);
    }

    @Override
    public void log(Throwable t, String message, Object... args) {
        nativeLogger.log(null, FQCN, LEVEL_MAP.get(this.level), message, args, t);
    }

    @Override
    public void log(Throwable t, String message, Supplier<?>... args) {
        if (isLevelDisabled()) {
            return;
        }
        nativeLogger.log(null,
                FQCN,
                LEVEL_MAP.get(this.level),
                message,
                Arrays.stream(args).map(Supplier::get).toArray(Object[]::new),
                t);
    }

    private Logger atLevel(Level level) {
        if (this.level == level) {
            return this;
        }
        return level == OFF ? NoopLogger.INSTANCE : getLogger(this.name, level);
    }

    private boolean isLevelDisabled() {
        switch (this.level) {
            case TRACE:
                return !nativeLogger.isTraceEnabled();
            case DEBUG:
                return !nativeLogger.isDebugEnabled();
            case INFO:
                return !nativeLogger.isInfoEnabled();
            case WARN:
                return !nativeLogger.isWarnEnabled();
            case ERROR:
                return !nativeLogger.isErrorEnabled();
            default:
                return true;
        }
    }

    private static class CallingStackUtil {
        static final String ELF4J_LOGGER_FACTORY_METHOD_NAME = "instance";
        static final String ELF4J_LOGGER_TYPE_NAME = elf4j.Logger.class.getName();

        static String getElf4jLoggerClientClassName() {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (int i = 0; i < stackTrace.length; i++) {
                StackTraceElement stackTraceElement = stackTrace[i];
                if (ELF4J_LOGGER_TYPE_NAME.equals(stackTraceElement.getClassName())
                        && ELF4J_LOGGER_FACTORY_METHOD_NAME.equals(stackTraceElement.getMethodName())) {
                    return stackTrace[i + 1].getClassName();
                }
            }
            throw new IllegalStateException(
                    "unable to locate ELF4J logger client class in calling stack: " + Arrays.toString(stackTrace));
        }
    }
}

