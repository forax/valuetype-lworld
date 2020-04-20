package fr.umlv.valuetype.persistent;

import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

@__inline__
public final class PersistentMap<K, V> implements Map<K, V> {
  private static final int DEFAULT_SIZE = 16;

  private final Thread ownerThread;
  private final GenEntry<K, V>[] entries;
  private final int size;

  private PersistentMap(Thread ownerThread, GenEntry<K, V>[] entries, int size) {
    this.ownerThread = ownerThread;
    this.entries = entries;
    this.size = size;
  }

  public interface GenEntry<K, V> extends Map.Entry<K, V> {
    K key();

    V value();

    int generation();

    GenEntry<K, V> with(K key, V value, int gen);

    @Override
    default V setValue(V value) {
      throw new UnsupportedOperationException();
    }
  }

  public static <K, V> PersistentMap<K, V> fromArrayCreator(
      IntFunction<? extends GenEntry<K, V>[]> arrayCreator) {
    var array = arrayCreator.apply(0);
    if (array.length != 0 || !array.getClass().getComponentType().isInlineClass()) {
      throw new IllegalArgumentException("array creator not implemented correctly");
    }
    return new PersistentMap<>(currentThread(), array, 0);
  }

  private static void checkOwnerThread(Thread ownerThread) {
    if (currentThread() != ownerThread) {
      throw new IllegalStateException("invalid owner thread");
    }
  }

  public PersistentMap<K, V> append(K key, V value) {
    requireNonNull(key);
    requireNonNull(value);
    checkOwnerThread(ownerThread);
    var length = entries.length;
    if (length == 0) {
      return oneEntry(key, value);
    }

    var index = key.hashCode() & (length - 1);
    for (; ; index = (index + 1) & (length - 1)) {
      var entry = entries[index];
      var entryKey = entry.key();
      if (entryKey == null) {
        break;
      }
      if (key.equals(entryKey)) {
        return replace(index, key, value, size);
      }
    }

    entries[index] = entries[index].with(key, value, size);
    if (size == length << 1) {
      return resize(key, value, size);
    }
    return new PersistentMap<>(ownerThread, entries, size + 1);
  }

  private PersistentMap<K, V> oneEntry(K key, V value) {
    var newEntries = Arrays.copyOf(entries, DEFAULT_SIZE);
    var index = key.hashCode() & (DEFAULT_SIZE - 1);
    newEntries[index] = newEntries[index].with(key, value, 0);
    return new PersistentMap<>(ownerThread, newEntries, 1);
  }

  private PersistentMap<K, V> replace(int index, K key, V value, int size) {
    var oldEntry = entries[index];
    if (value.equals(oldEntry.value())) {
      return this;
    }
    @SuppressWarnings("unchecked")
    var newEntries =
        (GenEntry<K, V>[]) Array.newInstance(entries.getClass().getComponentType(), entries.length);
    for (var i = 0; i < newEntries.length; i++) {
      var entry = entries[i];
      newEntries[i] = entry.with(entry.key(), entry.value(), 0); // reset generation
    }
    newEntries[index] = oldEntry.with(key, value, 0);
    return new PersistentMap<>(ownerThread, newEntries, size);
  }

  private PersistentMap<K, V> resize(K key, V value, int size) {
    var newLength = size << 2;
    @SuppressWarnings("unchecked")
    var newEntries =
        (GenEntry<K, V>[]) Array.newInstance(entries.getClass().getComponentType(), newLength);

    loop:
    for (var entry : entries) {
      if (entry.key() != null) {
        var index = key.hashCode() & (newLength - 1);
        for (; ; ) {
          if (newEntries[index].key() == null) {
            newEntries[index] = entry.with(key, value, size);
            continue loop;
          }
          index = (index + 1) & (newLength - 1);
        }
      }
    }
    return new PersistentMap<>(ownerThread, newEntries, size + 1);
  }

  @Override
  public int size() {
    checkOwnerThread(ownerThread);
    return size;
  }

  @Override
  public boolean isEmpty() {
    checkOwnerThread(ownerThread);
    return size == 0;
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    checkOwnerThread(ownerThread);
    return new MapEntrySet<>(entries, size, identity(), ownerThread);
  }

  @Override
  public Set<K> keySet() {
    checkOwnerThread(ownerThread);
    return new MapEntrySet<>(entries, size, GenEntry::key, ownerThread);
  }

  @Override
  public Collection<V> values() {
    checkOwnerThread(ownerThread);
    return new MapEntrySet<>(entries, size, GenEntry::value, ownerThread);
  }

  @__inline__
  private static final class MapEntrySet<K, V, R> implements Set<R> {
    private final GenEntry<K, V>[] entries;
    private final int size;
    private final Function<? super GenEntry<K, V>, ? extends R> mapper;
    private final Thread ownerThread;

    private MapEntrySet(
        GenEntry<K, V>[] entries,
        int size,
        Function<? super GenEntry<K, V>, ? extends R> mapper,
        Thread ownerThread) {
      this.entries = entries;
      this.size = size;
      this.mapper = mapper;
      this.ownerThread = ownerThread;
    }

    @Override
    public int size() {
      checkOwnerThread(ownerThread);
      return size;
    }

    @Override
    public boolean isEmpty() {
      checkOwnerThread(ownerThread);
      return size == 0;
    }

    @Override
    public boolean contains(Object o) {
      requireNonNull(o);
      checkOwnerThread(ownerThread);
      return Arrays.stream(entries)
          .filter(entry -> entry.generation() < size)
          .anyMatch(entry -> mapper.apply(entry).equals(o));
    }

    @Override
    public Iterator<R> iterator() {
      checkOwnerThread(ownerThread);
      return new MapEntryIterator<>(entries, size, mapper);
    }

    @Override
    public Object[] toArray() {
      checkOwnerThread(ownerThread);
      return Arrays.stream(entries)
          .filter(entry -> entry.generation() < size)
          .map(mapper)
          .toArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
      requireNonNull(a);
      checkOwnerThread(ownerThread);
      var array = a;
      if (a.length < size) {
        array = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
      }
      var iterator = iterator();
      for (var i = 0; i < size; i++) {
        array[i] = (T) iterator.next();
      }
      if (a.length >= size) {
        array[size] = null;
      }
      return array;
    }

    @Override
    public boolean add(R r) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      var set = new HashSet<R>(size);
      iterator().forEachRemaining(set::add);
      return set.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends R> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    private static final class MapEntryIterator<K, V, R> implements Iterator<R> {
      private final GenEntry<K, V>[] entries;
      private final int size;
      private final Function<? super GenEntry<K, V>, ? extends R> mapper;
      private int index;

      private MapEntryIterator(
          GenEntry<K, V>[] entries,
          int size,
          Function<? super GenEntry<K, V>, ? extends R> mapper) {
        this.entries = entries;
        this.size = size;
        this.mapper = mapper;
        index = nextIndex(0);
      }

      private int nextIndex(int index) {
        for (var i = index; i < size; i++) {
          var entry = entries[i];
          if (entry.key() != null && entry.generation() < size) {
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
      public R next() {
        if (!(hasNext())) {
          throw new NoSuchElementException("no element");
        }
        var entry = entries[index];
        index = nextIndex(index + 1);
        return mapper.apply(entry);
      }
    }
  }

  @Override
  public V get(Object key) {
    return getOrDefault(key, null); // V.default ??
  }

  @Override
  public V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public V getOrDefault(Object key, V defaultValue) {
    requireNonNull(key);
    checkOwnerThread(ownerThread);
    var entries = this.entries;
    var generation = size;
    var index = key.hashCode() & (entries.length - 1);
    for (; ; index = (index + 1) & (entries.length - 1)) {
      var entry = entries[index];
      if (entry.key() == null) {
        return defaultValue;
      }
      if (entry.key().equals(key) && entry.generation() < generation) {
        return entry.value();
      }
    }
  }

  @Override
  public boolean containsKey(Object key) {
    requireNonNull(key);
    checkOwnerThread(ownerThread);
    var entries = this.entries;
    var generation = size;
    var index = key.hashCode() & (entries.length - 1);
    for (; ; index = (index + 1) & (entries.length - 1)) {
      var entry = entries[index];
      if (entry.key() == null) {
        return false;
      }
      if (entry.key().equals(key) && entry.generation() < generation) {
        return true;
      }
    }
  }

  @Override
  public boolean containsValue(Object value) {
    requireNonNull(value);
    checkOwnerThread(ownerThread);
    return Arrays.stream(entries, 0, size)
        .filter(entry -> entry.key() != null && entry.generation() < size)
        .anyMatch(entry -> entry.value().equals(value));
  }
}
