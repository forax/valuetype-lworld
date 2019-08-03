package fr.umlv.valuetype;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class SmallStringTest {
	@Test
	void testFromInstanceof() {
		assertAll(
				() -> assertTrue(SmallString.from("") instanceof SmallString),
				() -> assertTrue(SmallString.from("a")  instanceof SmallString),
				() -> assertTrue(SmallString.from("aa")  instanceof SmallString),
				() -> assertTrue(SmallString.from("aaa")  instanceof SmallString),
				() -> assertTrue(SmallString.from("aaaa")  instanceof SmallString),
				() -> assertTrue(SmallString.from("aaaaa")  instanceof SmallString),
				() -> assertTrue(SmallString.from("aaaaaa")  instanceof SmallString),
				() -> assertTrue(SmallString.from("aaaaaaa")  instanceof SmallString),
				() -> assertTrue(SmallString.from("aaaaaaaa") instanceof String)
				);
	}
	@Test
	void testFromNot8Bits() {
		var texts =
				Stream.of("Ġ", "1Ġ", "12Ġ", "123Ġ", "1234Ġ", "12345Ġ", "123456Ġ");
		assertAll(texts.map(text -> () -> assertTrue(SmallString.from(text) instanceof String)));
	}
	
	@Test
	void testCharAt() {
		var texts =
				Stream.of("1", "12", "123", "1234", "12345", "123456", "1234567");
		assertAll(texts.map(text -> () -> assertEquals('1', SmallString.from(text).charAt(0))));
	}
	
	@Test
	void testCharAtAll() {
		var texts =
				Stream.of("", "1", "12", "123", "1234", "12345", "123456", "1234567");
		assertAll(texts.map(text -> {
			var ss = (SmallString)SmallString.from(text);
			return () -> assertEquals(text, range(0, ss.length()).mapToObj(i -> "" + ss.charAt(i)).collect(joining()));
		}));
	}

	@Test
	void testLength() {
		var texts =
				List.of("", "1", "12", "123", "1234", "12345", "123456", "1234567");
		assertAll(range(0, texts.size()).mapToObj(i -> () -> assertEquals(i, SmallString.from(texts.get(i)).length())));
	}

	@Test
	void testSubSequence() {
		var texts =
				Stream.of("1", "12", "123", "1234", "12345", "123456", "1234567");
		assertAll(texts.map(text -> () -> assertEquals("1", SmallString.from(text).subSequence(0, 1).toString())));
	}

	@Test
	void testToString() {
		var texts =
				Stream.of("1", "12", "123", "1234", "12345", "123456", "1234567");
		assertAll(texts.map(text -> () -> assertEquals(text, SmallString.from(text).toString())));
	}
}
