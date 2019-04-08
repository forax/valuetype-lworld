package fr.umlv.valuetype;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Dict<K, V> {
  private final int size;
  private final Entry<K, V>[] array;
  
  private Dict(int size, Entry<K,V>[] array) {
    this.size = size;
    this.array = array;
  }
  
  @SuppressWarnings("unchecked")
  public Dict() {
    this(0, (Entry<K, V>[])new Entry<?, ?>[8]);
  }
  
  @__value__
  private static /*value*/ class Entry<K, V> {
    private final int gen;
    private final K key;
    private final V value;
    
    private Entry(int gen, K key, V value) {
      this.gen = gen;
      this.key = key;
      this.value = value;
    }
  }
  
  public int size() {
    return size;
  }
  
  public Dict<K,V> append(K key, V value) {
    var index = key.hashCode() & (array.length - 1);
    var entry = array[index];
    for(;;) {
      if (entry.gen == 0) {
        array[index] = new Entry<>(size + 1, key, value);
        if (size * 2 == array.length) {
          return rehash(key, value, size + 1);
        }
        return new Dict<>(size + 1, array);
      }
      if (entry.gen < size && key.equals(entry.key)) {
        return this;
      } 
      index = (index + 1) & (array.length - 1);
      entry = array[index];
    }
  }
  
  private Dict<K, V> rehash(K key, V value, int size) {
    @SuppressWarnings("unchecked")
    var newArray = (Entry<K, V>[])new Entry<?, ?>[array.length * 2];
    var count = 0;
    loop: for(var i = 0; i < array.length; i++) {
      var entry = array[i];
      if (entry.gen == 0) {
        continue;
      }
      if (entry.gen < size) {
        var newIndex = entry.key.hashCode() & (newArray.length - 1);
        var newEntry = newArray[newIndex];
        for(;;) {
          if (newEntry.gen == 0) {
            newArray[newIndex] = entry;
            continue loop;
          }
          newIndex = (newIndex + 1) & (newArray.length - 1);
          newEntry = newArray[newIndex];
        }
      }
    }
    return new Dict<>(size, newArray);
  }
  
  public Option<V> find(K key) {
    var index = key.hashCode() & (array.length - 1);
    var entry = array[index];
    for(;;) {
      if (entry.gen == 0) {
        return Option.empty();
      }
      if (entry.gen < size && key.equals(entry.key)) {
        return Option.of(entry.value);
      } 
      index = (index + 1) & (array.length - 1);
      entry = array[index];
    }
  }
  
  @Override
  public String toString() {
    return Arrays.stream(array).filter(e -> e.gen != 0 && e.gen < size).map(e -> e.key + ": " + e.value).collect(Collectors.joining(", ", "{", "}"));
  }
  
  public static void main(String[] args) {
    var dict = new Dict<String, Integer>();
    var dict2 = dict.append("foo", 0);
    var dict3 = dict2.append("baz", 1);
    var dict4 = dict2.append("whizz", 2);
    System.out.println(dict3);
    System.out.println(dict4);
  }
}
