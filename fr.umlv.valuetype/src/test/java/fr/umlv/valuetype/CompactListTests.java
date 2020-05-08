package fr.umlv.valuetype;

import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@SuppressWarnings("static-method")
class CompactListTests {
	@Test
	void testOf() {
		var lists = List.of(
				CompactList.of(), CompactList.of("foo"), CompactList.of("foo", "bar"),
				CompactList.of("foo", "bar", "baz"), CompactList.of("foo", "bar", "baz", "wizz"),
				CompactList.of("foo", "bar", "baz", "wizz", "buzz")
				);
		for(var list: lists) {
			Assertions.assertNotNull(list);
		}
	}
	
	@Test
	void testOfNull() {
		Stream<Supplier<CompactList.ref<String>>> suppliers =
		  Stream.of(
				() -> CompactList.of((String)null),
				() -> CompactList.of("foo", null), () -> CompactList.of(null, "bar"),
				() -> CompactList.of(null, "bar", "baz"), () -> CompactList.of("foo", null, "baz"), () -> CompactList.of("foo", "bar", null),
				() -> CompactList.of("foo", "bar", "baz", null), () -> CompactList.of("foo", null, "baz", "wizz"), () -> CompactList.of("foo", "bar", null, "wizz"), () -> CompactList.of("foo", "bar", "baz", null),
				() -> CompactList.of(null, "bar", "baz", "wizz", "buzz"), () -> CompactList.of("foo", null, "baz", "wizz", "buzz"), () -> CompactList.of("foo", "bar", null, "wizz", "buzz"), () -> CompactList.of("foo", "bar", "baz", null, "buzz"), () -> CompactList.of("foo", "bar", "baz", "wizz", null),
				() -> CompactList.of((String[])null)
				);
		assertAll(suppliers.map(supplier -> () -> assertThrows(NullPointerException.class, () -> supplier.get())));
	}
	
	@Test
	void testSizeWithStrings() {
		var lists = List.of(
				CompactList.of(), CompactList.of("foo"), CompactList.of("foo", "bar"),
				CompactList.of("foo", "bar", "baz"), CompactList.of("foo", "bar", "baz", "wizz"),
				CompactList.of("foo", "bar", "baz", "wizz", "buzz")
				);
    assertAll(IntStream.range(0, lists.size()).mapToObj(i -> () -> assertEquals(i, lists.get(i).size())));		
	}
	@Test
	void testSizeWithInts() {
		var lists = List.of(
				CompactList.of(), CompactList.of(1), CompactList.of(1, 2),
				CompactList.of(1, 2, 3), CompactList.of(1, 2, 3, 4),
				CompactList.of(1, 2, 3, 4, 5)
				);
    assertAll(IntStream.range(0, lists.size()).mapToObj(i -> () -> assertEquals(i, lists.get(i).size())));		
	}
	@Test
	void testOfArraySize() {
		var lists = List.of(
				List.of(), List.of("foo"), List.of("foo", "bar"),
				List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz", "wizz"),
				List.of("foo", "bar", "baz", "wizz", "buzz")
				);
    assertAll(range(0, lists.size()).mapToObj(i -> () -> {
			var compactList = CompactList.of(lists.get(i).toArray(String[]::new));
			assertEquals(i, compactList.size());
		}));		
	}
	
	@Test
	void testGet() {
		var lists = Stream.of(
				CompactList.of("foo"), CompactList.of("foo", "bar"),
				CompactList.of("foo", "bar", "baz"), CompactList.of("foo", "bar", "baz", "wizz"),
				CompactList.of("foo", "bar", "baz", "wizz", "buzz")
				);
    assertAll(lists.map((Object list) -> () -> assertEquals("foo", (((CompactList<?>)list).get(0)))));  //FIXME Object
	}
	@Test
	void testGetAllElements() {
		var lists = Stream.of(
				List.of(), List.of("foo"), List.of("foo", "bar"),
				List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz", "wizz"),
				List.of("foo", "bar", "baz", "wizz", "buzz")
				);
    assertAll(lists.flatMap(list -> {
    	var compactList = CompactList.of(list.toArray(String[]::new));
    	return range(0, list.size()).mapToObj(i -> () -> assertEquals(list.get(i), compactList.get(i)));
    }));
	}

	@Test
	void testToString() {
		var lists = Stream.of(
				List.of(), List.of("foo"), List.of("foo", "bar"),
				List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz", "wizz"),
				List.of("foo", "bar", "baz", "wizz", "buzz")
				);
    assertAll(lists.map(list -> () -> {
			var compactList = CompactList.of(list.toArray(String[]::new));
			assertEquals(list.toString(), compactList.toString());
		}));
	}
	
	@Test
	void testEquals() {
		var lists = Stream.of(
				List.of(), List.of("foo"), List.of("foo", "bar"),
				List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz", "wizz"),
				List.of("foo", "bar", "baz", "wizz", "buzz")
				);
		assertAll(lists.map(list -> () -> {
			var compactList1 = CompactList.of(list.toArray(String[]::new));
			var compactList2 = CompactList.of(list.toArray(String[]::new));
			assertEquals(compactList1, compactList2);
		}));
	}
	
	@Test
	void testHashCode() {
		var lists = Stream.of(
				List.of(), List.of("foo"), List.of("foo", "bar"),
				List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz", "wizz"),
				List.of("foo", "bar", "baz", "wizz", "buzz")
				);
		assertAll(lists.map(list -> () -> {
			var compactList = CompactList.of(list.toArray(String[]::new));
			assertEquals(list.hashCode(), compactList.hashCode());
		}));
	}
	
	@Test
	void testIterator() {
		var lists = Stream.of(
				List.of(), List.of("foo"), List.of("foo", "bar"),
				List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz", "wizz"),
				List.of("foo", "bar", "baz", "wizz", "buzz")
				);
		assertAll(lists.map(list -> () ->  {
			//var compactList = CompactList.of(list.toArray(String[]::new));  BUG
			Iterable<String> compactList = CompactList.of(list.toArray(String[]::new));
			var arrayList = new ArrayList<String>();
			for(var element: compactList) {
				arrayList.add(element);
			}
			assertEquals(list, arrayList);
		}));
	}

	@Test
	void testAppend() {
		var compacts = range(0, 10).mapToObj(i -> CompactList.of(nCopies(i, 42).toArray(Integer[]::new))).collect(toList());
		assertAll(range(0, compacts.size() - 1).boxed().flatMap(i -> {
			var compact = compacts.get(i);
			var text = compact.toString();
			return Stream.of(
					() -> assertEquals(compacts.get(i + 1), compact.append(42)),
					() -> assertEquals(text, compact.toString())   // no side effect
					);
		}));
	}
	
	@Test
	void testToArray() {
		var lists = Stream.of(
				List.of(), List.of("foo"), List.of("foo", "bar"),
				List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz", "wizz"),
				List.of("foo", "bar", "baz", "wizz", "buzz")
				);
		assertAll(lists.map(list -> () -> {
			var compact = CompactList.of(list.toArray(String[]::new));
			assertEquals(list, List.of(compact.toArray(String[]::new)));
		}));
	}

	@Test
	void testOfArrayChangeAfterCreation() {
		String[] array = { "foo", "bar", "baz", "wizz", "buzz" };
		var compactList = CompactList.of(array);
		array[0] = "hell";
		assertEquals("[foo, bar, baz, wizz, buzz]", compactList.toString());
	}
}
