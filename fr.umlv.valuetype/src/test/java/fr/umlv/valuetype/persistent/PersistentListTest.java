package fr.umlv.valuetype.persistent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"WeakerAccess", "ResultOfMethodCallIgnored"})
public class PersistentListTest {
  @Test
  public void ofZero() {
    List<String> list = PersistentList.of();
    assertAll(
        () -> assertEquals(list, list),
        () -> assertEquals(List.of(), list),
        () -> assertEquals(List.of().hashCode(), list.hashCode()),
        () -> assertEquals("[]", list.toString()),
        () -> assertEquals(0, list.size()),
        () -> assertTrue(list.isEmpty()),
        () -> assertFalse(list.contains("foo")),
        () -> assertFalse(list.containsAll(List.of("foo", "bar"))),
        () -> assertTrue(list.containsAll(List.of())),
        () -> assertEquals(-1, list.indexOf("foo")),
        () -> assertEquals(-1, list.lastIndexOf("foo")));
  }

  @Test
  public void ofOne() {
    var list = PersistentList.of("foo");
    assertAll(
        () -> assertEquals(list, list),
        () -> assertEquals(List.of("foo"), list),
        () -> assertEquals(List.of("foo").hashCode(), list.hashCode()),
        () -> assertEquals("[foo]", list.toString()),
        () -> assertEquals(1, list.size()),
        () -> assertFalse(list.isEmpty()),
        () -> assertEquals("foo", list.get(0)),
        () -> assertTrue(list.contains("foo")),
        () -> assertFalse(list.contains("hello")),
        () -> assertFalse(list.containsAll(List.of("hello", "hello2"))),
        () -> assertTrue(list.containsAll(List.of("foo", "foo"))),
        () -> assertEquals(0, list.indexOf("foo")),
        () -> assertEquals(-1, list.indexOf("hello")),
        () -> assertEquals(0, list.lastIndexOf("foo")),
        () -> assertEquals(-1, list.lastIndexOf("hello")));
  }

  @Test
  public void ofTwo() {
    var list = PersistentList.of("foo", "bar");
    assertAll(
        () -> assertEquals(list, list),
        () -> assertEquals(List.of("foo", "bar"), list),
        () -> assertEquals(List.of("foo", "bar").hashCode(), list.hashCode()),
        () -> assertEquals("[foo, bar]", list.toString()),
        () -> assertEquals(2, list.size()),
        () -> assertFalse(list.isEmpty()),
        () -> assertEquals("foo", list.get(0)),
        () -> assertEquals("bar", list.get(1)),
        () -> assertTrue(list.contains("bar")),
        () -> assertFalse(list.contains("hello")),
        () -> assertFalse(list.containsAll(List.of("hello", "hello2"))),
        () -> assertTrue(list.containsAll(List.of("bar", "foo"))),
        () -> assertEquals(1, list.indexOf("bar")),
        () -> assertEquals(-1, list.indexOf("hello")),
        () -> assertEquals(1, list.lastIndexOf("bar")),
        () -> assertEquals(-1, list.lastIndexOf("hello")));
  }

  @Test
  public void ofTwoIdentical() {
    var list = PersistentList.of("foo", "foo");
    assertAll(
        () -> assertEquals(list, list),
        () -> assertEquals(List.of("foo", "foo"), list),
        () -> assertEquals(List.of("foo", "foo").hashCode(), list.hashCode()),
        () -> assertEquals("[foo, foo]", list.toString()),
        () -> assertEquals(2, list.size()),
        () -> assertFalse(list.isEmpty()),
        () -> assertEquals("foo", list.get(0)),
        () -> assertEquals("foo", list.get(1)),
        () -> assertTrue(list.contains("foo")),
        () -> assertFalse(list.contains("hello")),
        () -> assertFalse(list.containsAll(List.of("hello", "hello2"))),
        () -> assertTrue(list.containsAll(List.of("foo", "foo"))),
        () -> assertEquals(0, list.indexOf("foo")),
        () -> assertEquals(-1, list.indexOf("hello")),
        () -> assertEquals(1, list.lastIndexOf("foo")),
        () -> assertEquals(-1, list.lastIndexOf("hello")));
  }

  @Test
  public void ownership() {
    var list = PersistentList.of("foo", "bar");
    new Thread(
        () -> assertThrows(IllegalStateException.class, () -> list.append("baz"))
    ).start();
  }

  @Test
  public void from() {}

  @Test
  public void generate() {}

  @Test
  public void append() {}

  @Test
  public void unsupportedOperations() {
    var list = PersistentList.of("foo", "bar");
    assertAll(
        () -> assertThrows(UnsupportedOperationException.class, () -> list.add("hello")),
        () -> assertThrows(UnsupportedOperationException.class, () -> list.add(0, "hello")),
        () -> assertThrows(UnsupportedOperationException.class, list::clear),
        () -> assertThrows(UnsupportedOperationException.class, () -> list.add(0, "hello")),
        () -> assertThrows(UnsupportedOperationException.class, () -> list.replaceAll(x -> "a")),
        () ->
            assertThrows(UnsupportedOperationException.class, () -> list.addAll(List.of("hello"))),
        () ->
            assertThrows(
                UnsupportedOperationException.class, () -> list.addAll(0, List.of("hello"))),
        () -> assertThrows(UnsupportedOperationException.class, () -> list.remove("hello")),
        () -> assertThrows(UnsupportedOperationException.class, () -> list.remove(0)),
        () ->
            assertThrows(
                UnsupportedOperationException.class, () -> list.removeAll(List.of("hello"))),
        () -> assertThrows(UnsupportedOperationException.class, () -> list.removeIf(__ -> true)),
        () ->
            assertThrows(
                UnsupportedOperationException.class, () -> list.retainAll(List.of("hello"))),
        () -> assertThrows(UnsupportedOperationException.class, () -> list.set(0, "hello")),
        () -> assertThrows(UnsupportedOperationException.class, () -> list.sort(null)),
        () -> assertThrows(UnsupportedOperationException.class, () -> list.iterator().remove()));
  }

  @Test
  @SuppressWarnings("MagicNumber")
  public void iterator() {
    var list = PersistentList.of(12, 89, 56);
    var it = list.iterator();
    assertTrue(it.hasNext());
    assertEquals(12, (int)it.next());
    assertTrue(it.hasNext());
    assertEquals(89, (int)it.next());
    assertTrue(it.hasNext());
    assertEquals(56, (int)it.next());
    assertFalse(it.hasNext());
  }

  @Test
  @SuppressWarnings("MagicNumber")
  public void cursor() {
    var list = PersistentList.of(12, 89, 56);
    var cursor = list.cursor();
    assertTrue(cursor.hasNext());
    assertEquals(12, (int)cursor.element());
    cursor = cursor.next();

    assertTrue(cursor.hasNext());
    assertEquals(89, (int)cursor.element());
    cursor = cursor.next();

    assertTrue(cursor.hasNext());
    assertEquals(56, (int)cursor.element());
    cursor = cursor.next();

    assertFalse(cursor.hasNext());
    var _cursor = cursor;
    assertThrows(NoSuchElementException.class, _cursor::element);
    assertThrows(IllegalStateException.class, _cursor::next);
  }

  @Test
  @SuppressWarnings("MagicNumber")
  public void toObjectArray() {
    var array = PersistentList.of(12, 89, 56).toArray();
    assertSame(array.getClass(), Object[].class);
    assertEquals(List.of(12, 89, 56), List.of(array));
  }

  @Test
  @SuppressWarnings("MagicNumber")
  public void toAnyArray() {
    var list = PersistentList.<Object>of(12, 89, 56);
    assertAll(
        () -> {
          var array0 = list.toArray(new Integer[0]);
          assertEquals(List.of(12, 89, 56), List.of(array0));
        },
        () -> {
          var array1 = list.toArray(new Integer[1]);
          assertEquals(List.of(12, 89, 56), List.of(array1));
        },
        () -> {
          var array2 = list.toArray(new Integer[2]);
          assertEquals(List.of(12, 89, 56), List.of(array2));
        },
        () -> {
          var array3 = list.toArray(new Integer[3]);
          assertEquals(List.of(12, 89, 56), List.of(array3));
        },
        () -> {
          var array4 = list.toArray(new Integer[4]);
          assertEquals(Arrays.asList(12, 89, 56, null), Arrays.asList(array4));
        });
  }

  @Test
  public void get() {

  }

  @Test
  public void listIteratorNext() {
    var it = PersistentList.of("foo", "bar", "baz").listIterator();
    var it2 = List.of("foo", "bar", "baz").listIterator();
    while(it2.hasNext()) {
      assertTrue(it.hasNext());
      assertEquals(it2.nextIndex(), it.nextIndex());
      assertEquals(it2.previousIndex(), it.previousIndex());
      assertEquals(it2.next(), it.next());
    }
    assertFalse(it.hasNext());
    assertEquals(it2.nextIndex(), it.nextIndex());
    assertEquals(it2.previousIndex(), it.previousIndex());
  }

  @Test
  public void listIteratorPrevious() {
    var it = PersistentList.of("foo", "bar", "baz").listIterator(3);
    var it2 = List.of("foo", "bar", "baz").listIterator(3);
    while(it2.hasNext()) {
      assertTrue(it.hasNext());
      assertEquals(it2.nextIndex(), it.nextIndex());
      assertEquals(it2.previousIndex(), it.previousIndex());
      assertEquals(it2.next(), it.next());
    }
    assertFalse(it.hasNext());
    assertEquals(it2.nextIndex(), it.nextIndex());
    assertEquals(it2.previousIndex(), it.previousIndex());
  }

  @Test
  public void testListIterator() {}

  @Test
  public void subList() {}
}
