/*
 * MIT License
 *
 * Copyright (c) 2022 Qingtian Wang
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
 *
 */

package elf4j.logback;

import static elf4j.Level.*;

import elf4j.Level;
import elf4j.Logger;
import elf4j.util.NoopLogger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.annotation.concurrent.Immutable;
import lombok.NonNull;
import lombok.ToString;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

@Immutable
@ToString
class LogbackLogger implements Logger {
    private static final Level DEFAULT_LEVEL = INFO;
    private static final Class<Logger> SERVICE_ACCESS_CLASS = Logger.class;
    private static final String SERVICE_INTERFACE_CLASS = LogbackLogger.class.getName();
    private static final EnumMap<Level, Integer> LEVEL_MAP = setLeveMap();
    private static final EnumMap<Level, Map<String, LogbackLogger>> LOGGER_CACHE = initLoggerCache();

    @NonNull private final Level level;

    @NonNull private final String name;

    @NonNull private final ch.qos.logback.classic.Logger nativeLogger;

    private LogbackLogger(@NonNull String name, @NonNull Level level) {
        this.name = name;
        this.level = level;
        this.nativeLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
    }

    static LogbackLogger instance() {
        return getLogger(serviceClient().getClassName());
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

    private static StackTraceElement serviceClient() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String calleeClassName = SERVICE_ACCESS_CLASS.getName();
        for (int i = 0; i < stackTrace.length; i++) {
            if (calleeClassName.equals(stackTrace[i].getClassName())) {
                for (int j = i + 1; j < stackTrace.length; j++) {
                    if (!calleeClassName.equals(stackTrace[j].getClassName())) {
                        return stackTrace[j];
                    }
                }
                break;
            }
        }
        throw new NoSuchElementException("unable to locate caller class of " + SERVICE_ACCESS_CLASS + " in call stack "
                + Arrays.toString(stackTrace));
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

    private static Object supply(Object o) {
        return o instanceof Supplier<?> ? ((Supplier<?>) o).get() : o;
    }

    private static Object[] supply(Object[] objects) {
        return Arrays.stream(objects).map(LogbackLogger::supply).toArray();
    }

    @Override
    public Logger atLevel(Level level) {
        if (this.level == level) {
            return this;
        }
        return level == OFF ? NoopLogger.OFF : getLogger(this.name, level);
    }

    @Override
    public @NonNull Level getLevel() {
        return this.level;
    }

    @Override
    public boolean isEnabled() {
        switch (this.level) {
            case TRACE:
                return nativeLogger.isTraceEnabled();
            case DEBUG:
                return nativeLogger.isDebugEnabled();
            case INFO:
                return nativeLogger.isInfoEnabled();
            case WARN:
                return nativeLogger.isWarnEnabled();
            case ERROR:
                return nativeLogger.isErrorEnabled();
            default:
                return false;
        }
    }

    @Override
    public void log(Object message) {
        if (!this.isEnabled()) {
            return;
        }
        nativeLogger.log(
                null,
                SERVICE_INTERFACE_CLASS,
                LEVEL_MAP.get(this.level),
                Objects.toString(supply(message)),
                null,
                null);
    }

    @Override
    public void log(String message, Object... args) {
        if (!this.isEnabled()) {
            return;
        }
        nativeLogger.log(null, SERVICE_INTERFACE_CLASS, LEVEL_MAP.get(this.level), message, supply(args), null);
    }

    @Override
    public void log(Throwable t) {
        if (!this.isEnabled()) {
            return;
        }
        nativeLogger.log(null, SERVICE_INTERFACE_CLASS, LEVEL_MAP.get(this.level), t.getMessage(), null, t);
    }

    @Override
    public void log(Throwable t, Object message) {
        if (!this.isEnabled()) {
            return;
        }
        nativeLogger.log(
                null, SERVICE_INTERFACE_CLASS, LEVEL_MAP.get(this.level), Objects.toString(supply(message)), null, t);
    }

    @Override
    public void log(Throwable t, String message, Object... args) {
        if (!this.isEnabled()) {
            return;
        }
        nativeLogger.log(null, SERVICE_INTERFACE_CLASS, LEVEL_MAP.get(this.level), message, supply(args), t);
    }

    @NonNull String getName() {
        return name;
    }
}
