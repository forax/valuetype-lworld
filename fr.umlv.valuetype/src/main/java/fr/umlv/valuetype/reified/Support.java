package fr.umlv.valuetype.reified;

import static java.lang.invoke.MethodType.fromMethodDescriptorString;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;

public class Support {
  public interface Opcode<P, R> {
    R $(P parameter);
  }
  
  public interface Magic0 {
    <T> T $();
  }
  public interface Magic1 {
    <T> T $(Object o);
  }
  public interface Magic2 {
    <T> T $(Object o, Object o2);
  }
  public interface Magic3 {
    <T> T $(Object o, Object o2, Object o3);
  }
  public interface Magic4 {
    <T> T $(Object o, Object o2, Object o3, Object o4);
  }
  
  @SuppressWarnings("unused")
  private static Object trampoline(MethodHandle mh, Object parameter) {
    //System.out.println("trampoline0 with " + mh + " parameter " + parameter);
    try {
      return mh.invokeExact(parameter);
    } catch(RuntimeException | Error e) {
      throw e;
    } catch(Throwable t) {
      throw new AssertionError(t);
    }
  }
  @SuppressWarnings("unused")
  private static Object trampoline(MethodHandle mh, Object parameter, Object arg0) {
    //System.out.println("trampoline1 with " + mh + " arg0 " + arg0 + " parameter " + parameter);
    try {
      return mh.invokeExact(arg0, parameter);
    } catch(RuntimeException | Error e) {
      throw e;
    } catch(Throwable t) {
      throw new AssertionError(t);
    }
  }
  @SuppressWarnings("unused")
  private static Object trampoline(MethodHandle mh, Object parameter, Object arg0, Object arg1) {
    try {
      return mh.invokeExact(arg0, arg1, parameter);
    } catch(RuntimeException | Error e) {
      throw e;
    } catch(Throwable t) {
      throw new AssertionError(t);
    }
  }
  @SuppressWarnings("unused")
  private static Object trampoline(MethodHandle mh, Object parameter, Object arg0, Object arg1, Object arg2) {
    try {
      return mh.invokeExact(arg0, arg1, arg2, parameter);
    } catch(RuntimeException | Error e) {
      throw e;
    } catch(Throwable t) {
      throw new AssertionError(t);
    }
  }
  @SuppressWarnings("unused")
  private static Object trampoline(MethodHandle mh, Object parameter, Object arg0, Object arg1, Object arg2, Object arg3) {
    try {
      return mh.invokeExact(arg0, arg1, arg2, arg3, parameter);
    } catch(RuntimeException | Error e) {
      throw e;
    } catch(Throwable t) {
      throw new AssertionError(t);
    }
  }
  
  
  private static void checkArity(MethodType descType, Class<?> magicClass) {
    var magicClassName = magicClass.getName();
    if (descType.parameterCount() != magicClassName.charAt(magicClassName.length() - 1) - '0') {
      throw new IllegalArgumentException("magic class " + magicClassName + " as wrong arity for descriptor " + descType);
    }
  }
  
  private static <T> T asMagic(Class<T> interfaze, MethodType methodType, MethodHandle mh, Object parameter) {
    T result;
    try {
      result = interfaze.cast(LambdaMetafactory.
          metafactory(MethodHandles.lookup(), "$",
              methodType(interfaze, MethodHandle.class, Object.class),
              methodType,
              MethodHandles.lookup().findStatic(Support.class, "trampoline",
                                                methodType.insertParameterTypes(0, MethodHandle.class).appendParameterTypes(Object.class)),
              methodType)
          .getTarget()
          .invoke(mh, parameter));
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
    //System.out.println("result " + java.util.Arrays.toString(result.getClass().getMethods()));
    return result;
  }
  
  public static <P, M> Opcode<P, M> reified_new(Class<P> parameterClass, Class<M> magicClass, Class<?> clazz, String descriptor) {
    MethodType descType = fromMethodDescriptorString(descriptor, clazz.getClassLoader());
    checkArity(descType, magicClass);
    MethodHandle mh;
    try {
      mh = MethodHandles.lookup().findConstructor(clazz, descType.appendParameterTypes(parameterClass));
    } catch (NoSuchMethodException | IllegalAccessException | TypeNotPresentException e) {
      throw (LinkageError)new LinkageError().initCause(e);
    }
    MethodHandle target = mh.asType(mh.type().generic());
    MethodType magicDescType = descType.generic().changeReturnType(Object.class);
    return parameter -> asMagic(magicClass, magicDescType, target, parameter);
  }
  
  public static Opcode<Class<?>, Magic1> reified_checkcast() {
    MethodHandle mh;
    try {
      mh = MethodHandles.lookup().findVirtual(Class.class, "cast", methodType(Object.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException | TypeNotPresentException e) {
      throw (LinkageError)new LinkageError().initCause(e);
    }
    mh = MethodHandles.permuteArguments(mh, methodType(Object.class, Object.class, Class.class), new int[] {1, 0});
    MethodHandle target = mh.asType(mh.type().generic());
    return parameter -> asMagic(Magic1.class, methodType(Object.class, Object.class), target, parameter);
  }
  
  public static Opcode<Class<?>, Magic1> reified_new_array() {
    MethodHandle mh;
    try {
      mh = MethodHandles.lookup().findStatic(Array.class, "newInstance", methodType(Object.class, Class.class, int.class));
    } catch (NoSuchMethodException | IllegalAccessException | TypeNotPresentException e) {
      throw (LinkageError)new LinkageError().initCause(e);
    }
    mh = MethodHandles.permuteArguments(mh, methodType(Object.class, int.class, Class.class), new int[] {1, 0});
    MethodHandle target = mh.asType(mh.type().generic());
    return parameter -> asMagic(Magic1.class, methodType(Object.class, Object.class), target, parameter);
  }
  
  public static <P, M> Opcode<P, M> reified_invokevirtual(Class<P> parameterClass, Class<M> magicClass, Class<?> clazz, String name, String desc) {
    MethodType descType = fromMethodDescriptorString(desc, clazz.getClassLoader());
    MethodType magicDescType = descType.insertParameterTypes(0, Object.class).generic().changeReturnType(Object.class);
    checkArity(magicDescType, magicClass);
    MethodHandle mh;
    try {
      mh = MethodHandles.lookup().findVirtual(clazz, name, descType.appendParameterTypes(parameterClass));
    } catch (NoSuchMethodException | IllegalAccessException | TypeNotPresentException e) {
      throw (LinkageError)new LinkageError().initCause(e);
    }
    MethodHandle target = mh.asType(mh.type().generic());
    return parameter -> asMagic(magicClass, magicDescType, target, parameter);
  }
  
  public static <P, M> Opcode<P, Magic1> reified_invokestatic(Class<P> parameterClass, Class<M> magicClass, Class<?> clazz, String name, String desc) {
    MethodType descType = fromMethodDescriptorString(desc, clazz.getClassLoader());
    checkArity(descType, magicClass);
    MethodHandle mh;
    try {
      mh = MethodHandles.lookup().findStatic(clazz, name, descType.appendParameterTypes(parameterClass));
    } catch (NoSuchMethodException | IllegalAccessException | TypeNotPresentException e) {
      throw (LinkageError)new LinkageError().initCause(e);
    }
    MethodType magicDescType = descType.generic().changeReturnType(Object.class);
    MethodHandle target = mh.asType(mh.type().generic());
    return parameter -> asMagic(Magic1.class, magicDescType, target, parameter);
  }
}
