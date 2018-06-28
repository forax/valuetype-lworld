package fr.umlv.valuetype;

import java.util.Objects;
import java.util.function.Consumer;

public interface FluentLogger {
  public enum Level {
    DEBUG, WARNING, ERROR
  }
  
  public default MessageFluent atDebug() {
    return at(Level.DEBUG);
  }
  
  public default MessageFluent atWarning() {
    return at(Level.WARNING);
  }
  
  public default MessageFluent atError() {
    return at(Level.ERROR);
  }

  public MessageFluent at(Level level);
  
  public static FluentLogger create(Class<?> declaringClass, Level level) {
    Objects.requireNonNull(declaringClass);
    Objects.requireNonNull(level);
    return Impl.create(declaringClass.getName(), level, null);
  }
  
  public interface MessageFluent {
    public LogFluent message(String message);
  }
  
  public interface LogFluent {
    public default void log() {
      log(System.err::println);
    }
    public void log(Consumer<? super String> consumer);
  }
  
  static final __ByValue class Impl implements FluentLogger, MessageFluent, LogFluent  {
    private final String className;
    private final Level level;
    private final String message;

    public Impl() {
      this.className = null;
      this.level = null;
      this.message = null;
      throw new AssertionError();
    }
    
    public static Impl create(String className, Level level, String message) {
      Impl impl = __MakeDefault Impl();
      impl = __WithField(impl.className, className);
      impl = __WithField(impl.level, level);
      impl = __WithField(impl.message, message);
      return impl;
    }
    
    @Override
    public MessageFluent at(Level level) {
      Objects.requireNonNull(level);
      return create(className, isEnabled(this.level, level)? level: null, null);
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
      return create(className, level, message);
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
