package fr.umlv.valuetype;

import java.util.Map;
import java.util.Objects;

public final class FlatMap<K,V> {
  private Entry<K,V>[] entries;
  private int size;
  
  @SuppressWarnings("unchecked")
  public FlatMap() {
    this.entries = (Entry<K,V>[])new Entry<?,?>[16];
  }
  
  @__inline__
  private static final /*inline*/ class Entry<K,V> implements Map.Entry<K, V> {
    final int hash;
    final K key;
    final V value;
    
    private Entry(int hash, K key, V value) {
      this.hash = hash;
      this.key = key;
      this.value = value;
    }
    
    @Override
    public K getKey() {
      return key;
    }
    @Override
    public V getValue() {
      return value;
    }
    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }
  }
  
  public int size() {
    return size;
  }
  
  public Option<V> get(K key) {
    var hash = key.hashCode(); // implicit nullcheck
    var mask = entries.length - 1;
    int index = hash & mask;
    for(;;) {
      var entry = entries[index];
      if (hash == entry.hash && key.equals(entry.key)) {
        return Option.of(entry.value);
      }
      if (entry.key == null) {
        return Option.empty();
      }
      index = (index + 1) & mask;
    }
  }
  
  public void put(K key, V value) {
    var hash = key.hashCode(); // implicit nullcheck
    Objects.requireNonNull(value);
    var entries = this.entries;
    var length = entries.length;
    if (size == length >> 1) {
      entries = resize();
      length = entries.length;
    }
    var mask = length - 1;
    var index = hash & mask;
    for(;;) {
      var entry = entries[index];
      var empty = entry.key == null;
      if (empty || (hash == entry.hash && key.equals(entry.key))) {
        entries[index] = new Entry<>(hash, key, value);
        if (!empty) {
          return;
        }
        size++;
        return;
      }
      index = (index + 1) & mask;
    }
  }
  
  private Entry<K, V>[] resize() {
    @SuppressWarnings("unchecked")
    Entry<K, V>[] newEntries = (Entry<K, V>[])new Entry<?, ?>[entries.length << 1];
    int mask = newEntries.length - 1;
    for(var entry: entries) {
      if (entry.key == null) {
        continue;
      }
      for(var index = entry.key.hashCode() & mask;;index = (index + 1) & mask) {
        if (newEntries[index].key == null) {
          newEntries[index] = entry;
          break;
        }
      }
    }
    return entries = newEntries;
  }
}
