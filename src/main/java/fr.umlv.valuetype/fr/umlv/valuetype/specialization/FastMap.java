package fr.umlv.valuetype.specialization;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import fr.umlv.valuetype.Point;

public class FastMap<K, V> extends AbstractMap<K, V> {
  private final Species<FastMap<K, V>> __SPECIES__;
  private Entry<K, V>[] entries;
  private int size;
  
  @__inline__
  static /*inline*/ class Entry<K, V> implements Map.Entry<K, V>{
    private final Species<Entry<K, V>> __SPECIES__;
    private int hash;
    private K key;
    private V value;
    
    Entry(Species<K> _K, Species<V> _V, int hash, K key, V value) {
      this.__SPECIES__ = Species.raw(Entry.class).with(_K, _V);
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
  
  public FastMap(Species<K> _K, Species<V> _V) {
    this.__SPECIES__ = Species.raw(FastMap.class).with(_K, _V);
    entries = Species.raw(Entry.class).<Entry<K,V>>with(_K, _V).newArray(2);
  }

  @Override
  public V put(K key, V value) {
    Species<K> _K = __SPECIES__.getTypeArgument(0);
    Species<V> _V = __SPECIES__.getTypeArgument(1);
    _K.checkCast(key);
    _V.checkCast(value);
    
    var hashCode = key.hashCode() & 0x7FFFFFFF;
    var length = entries.length;
    var index = hashCode & (length - 1);
    
    var hash = hashCode | 0x80000000;
    for(var i = index; i < length; i++) {
      var entry = entries[i];
      var entryHash = entry.hash;
      if (entryHash == 0) {
        entries[i] = new Entry<>(_K, _V, hash, key, value);
        if (size++ == length >>> 1) {
          resize();
        }
        return null;
      }
      if (entryHash == hash && entry.key.equals(key)) {
        var oldValue = entry.value;
        entries[i] = new Entry<>(_K, _V, hash, key, value);
        return oldValue;
      }
    }
    for(var i = 0; i < index; i++) {
      var entry = entries[i];
      var entryHash = entry.hash;
      if (entryHash == 0) {
        entries[i] = new Entry<>(_K, _V, hash, key, value);
        if (size++ == length >>> 1) {
          resize();
        }
        return null;
      }
      if (entryHash == hash && entry.key.equals(key)) {
        var oldValue = entry.value;
        entries[i] = new Entry<>(_K, _V, hash, key, value);
        return oldValue;
      }
    }
    throw new AssertionError("hash map is full");
  }
  
  private void resize() {
    var oldLength = entries.length;
    var newLength = oldLength << 1;
    
    @SuppressWarnings("unchecked")
    Entry<K,V>[] newEntries = (Entry<K,V>[])Array.newInstance(entries.getClass().getComponentType(), newLength);
    
    loop: for(var j = 0; j < oldLength; j++) {
      var oldEntry = entries[j];
      var oldEntryHash = oldEntry.hash;
      if (oldEntryHash == 0) {
        continue loop;
      }
      
      var hashCode = oldEntryHash & 0x7FFFFFFF;
      var index = hashCode & (newLength - 1);
      
      for(var i = index; i < newLength; i++) {
        if (newEntries[i].hash == 0) {
          newEntries[i] = oldEntry;
          continue loop;
        }
      }
      for(var i = 0; i < index; i++) {
        if (newEntries[i].hash == 0) {
          newEntries[i] = oldEntry;
          continue loop;
        }
      }
      throw new AssertionError("hash map is full ??");
    }
    
    entries = newEntries;
  }
  
  @Override
  public V get(Object key) {
    __SPECIES__.getTypeArgument(0).checkCast(key);
    
    var hashCode = key.hashCode() & 0x7FFFFFFF;
    var index = hashCode & (entries.length - 1);
    
    var hash = hashCode | 0x80000000;
    for(var i = index; i < entries.length; i++) {
      var entry = entries[i];
      var entryHash = entry.hash;
      if (entryHash == 0) {
        return null;
      }
      if (entryHash == hash && entry.key.equals(key)) {
        return entry.value;
      }
    }
    for(var i = 0; i < index; i++) {
      var entry = entries[i];
      var entryHash = entry.hash;
      if (entryHash == 0) {
        return null;
      }
      if (entryHash == hash && entry.key.equals(key)) {
        return entry.value;
      }
    }
    return null;  // oh god !
  }
  
  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    class EntrySet extends AbstractSet<Map.Entry<K, V>> {
      private final Species<EntrySet> __SPECIES__;

      EntrySet(Species<K> _K, Species<V> _V) {
        this.__SPECIES__ = Species.raw(EntrySet.class).with(_K, _V);
      }

      @Override
      public Iterator<Map.Entry<K, V>> iterator() {
        class EntrySetIterator implements Iterator<Map.Entry<K, V>> {
          private final Species<EntrySetIterator> __SPECIES__;
          private int index = nextIndex(0);
          
          EntrySetIterator(Species<K> _K, Species<V> _V) {
            this.__SPECIES__ = Species.raw(EntrySetIterator.class).with(_K, _V);
          }
          
          private int nextIndex(int index) {
            for(var i = index; i < entries.length; i++) {
              if ((entries[i].hash & 0x80000000) != 0) {
                return i;
              }
            }
            return -1;
          }
          
          @Override
          public boolean hasNext() {
            return index != -1;
          }
          @Override
          public Map.Entry<K, V> next() {
            var index = this.index;
            this.index = nextIndex(index + 1);
            return entries[index];
          }
        }
        return new EntrySetIterator(__SPECIES__.getTypeArgument(0), __SPECIES__.getTypeArgument(1));
      }

      @Override
      public int size() {
        return size;
      }
    }
    return new EntrySet(__SPECIES__.getTypeArgument(0), __SPECIES__.getTypeArgument(1));
  }

  @Override
  public int size() {
    return size;
  }
  
  public static void main(String[] args) {
    System.out.println(Species.raw(FastMap.class).getTypeArgumentCount());
    
    var map = new FastMap<>(Species.raw(Point.class.asPrimaryType()), Species.raw(String.class));
    map.put(new Point(1, 2), "a");
    map.put(new Point(10, 20), "b");
    map.put(new Point(100, 200), "c");
    map.put(new Point(10, 20), "d");
    
    map.entrySet().forEach(System.out::println);
    
    //for(var i = 0; i < 10_000; i++) {
    //  map.put(new Point(i, i), "" + i);
    //}
  }
}
