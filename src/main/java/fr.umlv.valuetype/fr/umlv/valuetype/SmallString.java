package fr.umlv.valuetype;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public @__inline__ class SmallString implements CharSequence {
  private byte c0;
  private byte c1;
  private byte c2;
  private byte c3;
  private byte c4;
  private byte c5;
  private byte c6;
  private byte size;
  
	private SmallString(byte c0, byte c1, byte c2, byte c3, byte c4, byte c5, byte c6) {
		this.size = 7;
		this.c0 = c0;
		this.c1 = c1;
		this.c2 = c2;
		this.c3 = c3;
		this.c4 = c4;
		this.c5 = c5;
		this.c6 = c6;
	}
	private SmallString(byte c0, byte c1, byte c2, byte c3, byte c4, byte c5) {
		this.size = 6;
		this.c0 = c0;
		this.c1 = c1;
		this.c2 = c2;
		this.c3 = c3;
		this.c4 = c4;
		this.c5 = c5;
		//
		this.c6 = 0;
	}
	private SmallString(byte c0, byte c1, byte c2, byte c3, byte c4) {
		this.size = 5;
		this.c0 = c0;
		this.c1 = c1;
		this.c2 = c2;
		this.c3 = c3;
		this.c4 = c4;
		//
		this.c5 = 0;
		this.c6 = 0;
	}
	private SmallString(byte c0, byte c1, byte c2, byte c3) {
		this.size = 4;
		this.c0 = c0;
		this.c1 = c1;
		this.c2 = c2;
		this.c3 = c3;
		//
	  this.c4 = 0;
		this.c5 = 0;
		this.c6 = 0;
	}
	private SmallString(byte c0, byte c1, byte c2) {
		this.size = 3;
		this.c0 = c0;
		this.c1 = c1;
		this.c2 = c2;
		//
		this.c3 = 0;
	  this.c4 = 0;
		this.c5 = 0;
		this.c6 = 0;
	}
	private SmallString(byte c0, byte c1) {
		this.size = 2;
		this.c0 = c0;
		this.c1 = c1;
		//
		this.c2 = 0;
		this.c3 = 0;
	  this.c4 = 0;
		this.c5 = 0;
		this.c6 = 0;
	}
	private SmallString(byte c0) {
		this.size = 1;
		this.c0 = c0;
		//
		this.c1 = 0;
		this.c2 = 0;
		this.c3 = 0;
	  this.c4 = 0;
		this.c5 = 0;
		this.c6 = 0;
	}
	private SmallString() {
		//
		this.size = 0;
		this.c0 = 0;
		this.c1 = 0;
		this.c2 = 0;
		this.c3 = 0;
	  this.c4 = 0;
		this.c5 = 0;
		this.c6 = 0;
	}

	@Override
	public char charAt(int index) {
		Objects.checkIndex(index, size);
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException();
		}
		switch(index) {
		case 0:
			return (char)c0;
		case 1:
			return (char)c1;
		case 2:
			return (char)c2;
		case 3:
			return (char)c3;
		case 4:
			return (char)c4;
		case 5:
			return (char)c5;
		default: // case 6:
			return (char)c6;
		}
	}
	
	@Override
	public int length() {
		return size;
	}
	
	@Override
	public CharSequence subSequence(int start, int end) {
		Objects.checkFromToIndex(start, end, size);
		return from(toString().substring(start, end));
	}
	
	@Override
	public String toString() {
		return new String(asBytes(), StandardCharsets.ISO_8859_1);
	}
	
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	
	private byte[] asBytes() {
		switch(size) {
		case 0:
			return EMPTY_BYTE_ARRAY;
		case 1:
			return new byte[] { c0 };
		case 2:
			return new byte[] { c0, c1 };
		case 3:
			return new byte[] { c0, c1, c2 };
		case 4:
			return new byte[] { c0, c1, c2, c3 };
		case 5:
			return new byte[] { c0, c1, c2, c3, c4 };
		case 6:
			return new byte[] { c0, c1, c2, c3, c4, c5 };
		default: //case 7:
			return new byte[] { c0, c1, c2, c3, c4, c5, c6 };
		}
	}
	
	private static boolean isNot8bits(char c) {
		return (c & 0xFF00) != 0;
	}
	
	public static CharSequence from(CharSequence s) {
		switch(s.length()) {
		case 0:
			return from0();
		case 1:
			return from1(s);
		case 2:
			return from2(s);
		case 3:
			return from3(s);
		case 4:
			return from4(s);
		case 5:
			return from5(s);
		case 6:
			return from6(s);
		case 7:
			return from7(s);
	  default:
	  	return s;
		}
	}
	
  private static SmallString from0() {
  	return new SmallString();
  }
  private static CharSequence from1(CharSequence s) {
  	char c0 = s.charAt(0);
		if (isNot8bits(c0)) {
  	  return s;
		}
		return new SmallString((byte)c0);
  }
  private static CharSequence from2(CharSequence s) {
  	char c0 = s.charAt(0);
  	char c1 = s.charAt(1);
		if (isNot8bits(c0) | isNot8bits(c1)) {
  	  return s;
		}
		return new SmallString((byte)c0, (byte)c1);
  }
  private static CharSequence from3(CharSequence s) {
  	char c0 = s.charAt(0);
  	char c1 = s.charAt(1);
  	char c2 = s.charAt(2);
		if (isNot8bits(c0) | isNot8bits(c1) | isNot8bits(c2)) {
  	  return s;
		}
		return new SmallString((byte)c0, (byte)c1, (byte)c2);
  }
  private static CharSequence from4(CharSequence s) {
  	char c0 = s.charAt(0);
  	char c1 = s.charAt(1);
  	char c2 = s.charAt(2);
  	char c3 = s.charAt(3);
		if (isNot8bits(c0) | isNot8bits(c1) | isNot8bits(c2) | isNot8bits(c3)) {
  	  return s;
		}
		return new SmallString((byte)c0, (byte)c1, (byte)c2, (byte)c3);
  }
  private static CharSequence from5(CharSequence s) {
  	char c0 = s.charAt(0);
  	char c1 = s.charAt(1);
  	char c2 = s.charAt(2);
  	char c3 = s.charAt(3);
  	char c4 = s.charAt(4);
		if (isNot8bits(c0) | isNot8bits(c1) | isNot8bits(c2) | isNot8bits(c3) | isNot8bits(c4)) {
  	  return s;
		}
		return new SmallString((byte)c0, (byte)c1, (byte)c2, (byte)c3, (byte)c4);
  }
  private static CharSequence from6(CharSequence s) {
  	char c0 = s.charAt(0);
  	char c1 = s.charAt(1);
  	char c2 = s.charAt(2);
  	char c3 = s.charAt(3);
  	char c4 = s.charAt(4);
  	char c5 = s.charAt(5);
		if (isNot8bits(c0) | isNot8bits(c1) | isNot8bits(c2) | isNot8bits(c3) | isNot8bits(c4) | isNot8bits(c5)) {
  	  return s;
		}
		return new SmallString((byte)c0, (byte)c1, (byte)c2, (byte)c3, (byte)c4, (byte)c5);
  }
  private static CharSequence from7(CharSequence s) {
  	char c0 = s.charAt(0);
  	char c1 = s.charAt(1);
  	char c2 = s.charAt(2);
  	char c3 = s.charAt(3);
  	char c4 = s.charAt(4);
  	char c5 = s.charAt(5);
  	char c6 = s.charAt(6);
		if (isNot8bits(c0) | isNot8bits(c1) | isNot8bits(c2) | isNot8bits(c3) | isNot8bits(c4) | isNot8bits(c5) | isNot8bits(c6)) {
  	  return s;
		}
		return new SmallString((byte)c0, (byte)c1, (byte)c2, (byte)c3, (byte)c4, (byte)c5, (byte)c6);
  }
}
