package fr.umlv.valuetype;

public class ValueTearing {
	@__inline__
  private static final class Value {
		private int x;
		private int y;
		
		private Value(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
  public static void main(String[] args) {
		var array = new Value[1];
		var zero = new Value(0, 0);
		var one = new Value(1, 1);
		new Thread(() -> {
			for(;;) {
			  array[0] = zero;
			}
	  }).start();
		new Thread(() -> {
			for(;;) {
			  array[0] = one;
			}
	  }).start();
		for(;;) {
			var val = array[0];
			if (val != zero && val != one) {
				throw new AssertionError("oops");
			}
		}
	}
}
