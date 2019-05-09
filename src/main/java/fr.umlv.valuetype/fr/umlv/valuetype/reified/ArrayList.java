package fr.umlv.valuetype.reified;

import static fr.umlv.valuetype.reified.ArrayList.__CP.__CP;
import static fr.umlv.valuetype.reified.ArrayList.__MP0.__MP0;
import static fr.umlv.valuetype.reified.Support.*;

import fr.umlv.valuetype.IntBox;
import fr.umlv.valuetype.reified.Support.Magic0;
import fr.umlv.valuetype.reified.Support.Magic1;
import fr.umlv.valuetype.reified.Support.Magic2;
import fr.umlv.valuetype.reified.Support.Opcode;

public class ArrayList</*any*/E> {
  final static /*inline*/ class __CP {
    final Class<?> e;
    __CP(Class<?> e) { this.e = e; }
    static __CP __CP(Class<?> e) { return new __CP(e); }
  }
  final static /*inline*/ class __MP0 {
    final Class<?> t;
    public __MP0(Class<?> t) { this.t = t; }
    static __MP0 __MP0(Class<?> t) { return new __MP0(t); }
  }
  
  private static final Opcode<Class<?>, Magic1> reified_new_array = reified_new_array();
  private static final Opcode<Class<?>, Magic1> reified_checkcast_T = reified_checkcast();
  private static final Opcode<__CP, Magic1> reified_new_arraylist = reified_new(__CP.class, Magic1.class, ArrayList.class, "(I)V");
  private static final Opcode<__CP, Magic2> reified_invokevirtual_add = reified_invokevirtual(__CP.class, Magic2.class, ArrayList.class, "add", "(Ljava/lang/Object;)V");
  private static final Opcode<__CP, Magic2> reified_invokevirtual_get = reified_invokevirtual(__CP.class, Magic2.class, ArrayList.class, "get", "(I)Ljava/lang/Object;");
  private static final Opcode<__MP0, Magic1> reified_invokestatic_single = reified_invokestatic(__MP0.class, Magic1.class, ArrayList.class, "single", "(Ljava/*inline*/ArrayList;");
  
  // ---
  
  private int size;
  private E[] array;
  
  public ArrayList(int capacity, __CP cp) {
    array = reified_new_array.$(cp.e).$(capacity);
  }
  
  public void add(E element, __CP cp) {
    reified_checkcast_T.$(cp.e).$(element);
    array[size] = element;
  }
  
  public int size() {
    return size;
  }
  
  public E get(int index, __CP cp) {
    return array[index];
  }
  
  public static <T> ArrayList<T> single(T element, __MP0 mp0) {
    reified_checkcast_T.$(mp0.t).$(element);
    ArrayList<T> list = reified_new_arraylist.$(__CP(mp0.t)).$(1);
    reified_invokevirtual_add.$(__CP(mp0.t)).$(list, element);
    return list;
  }
  
  public static void main(String[] args) {
    ArrayList<String> list = reified_invokestatic_single.$(__MP0(String.class)).$("hello");
    String s = reified_invokevirtual_get.$(__CP(String.class)).$(list, 0);
    System.out.println(s);
    
    ArrayList<IntBox> list2 = reified_invokestatic_single.$(__MP0(IntBox.class)).$(IntBox.valueOf(42));
    IntBox box = reified_invokevirtual_get.$(__CP(IntBox.class)).$(list2, 0);
    System.out.println(box);
  }
}
