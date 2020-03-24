package fr.umlv.valuetype;

import java.util.Objects;
import java.util.function.Consumer;

@FunctionalInterface
public interface FluentLogger {
  enum Level {
    DEBUG, WARNING, ERROR
  }
  
  default MessageFluent atDebug() {
    return at(Level.DEBUG);
  }
  
  default MessageFluent atWarning() {
    return at(Level.WARNING);
  }
  
  default MessageFluent atError() {
    return at(Level.ERROR);
  }

  MessageFluent at(Level level);
  
  static FluentLogger create(Class<?> declaringClass, Level level) {
    Objects.requireNonNull(declaringClass);
    Objects.requireNonNull(level);
    return new Impl(declaringClass.getName(), level, null);
  }

  @FunctionalInterface
  interface MessageFluent {
    LogFluent message(String message);
  }

  @FunctionalInterface
  interface LogFluent {
    default void log() {
      log(System.err::println);
    }
    void log(Consumer<? super String> consumer);
  }
  
  @__inline__
  final /*inline*/ class Impl implements FluentLogger, MessageFluent, LogFluent  {
    private final String className;
    private final Level level;
    private final String message;

    public Impl(String className, Level level, String message) {
      this.className = className;
      this.level = level;
      this.message = message;
    }
    
    @Override
    public MessageFluent at(Level level) {
      Objects.requireNonNull(level);
      return new Impl(className, isEnabled(this.level, level)? level: null, null);
    }
    
    private static boolean isEnabled(Level loggerLevel, Level loggingLevel) {
      if (loggerLevel == Level.DEBUG) {
        return true;
      }
      if (loggerLevel == Level.WARNING) {
        return loggingLevel == Level.WARNING || loggingLevel == Level.ERROR;
      }
      //if (loggerLevel == Level.ERROR) {
        return loggingLevel == Level.ERROR;
      //}  
    }
    
    @Override
    public LogFluent message(String message) {
      Objects.requireNonNull(message);
      return new Impl(className, level, message);
    }
    
    @Override
    public void log(Consumer<? super String> consumer) {
      Objects.requireNonNull(consumer);
      if (level != null) {
        consumer.accept(level + " " + message + " at " + className);
      }
    }
  }
}
