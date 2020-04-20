package fr.umlv.valuetype;

import static java.lang.Math.addExact;
import static java.lang.Math.decrementExact;
import static java.lang.Math.incrementExact;
import static java.lang.Math.multiplyExact;
import static java.lang.Math.negateExact;
import static java.lang.Math.subtractExact;
import static java.math.BigInteger.valueOf;

import java.math.BigInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@__inline__
public /*inline*/ class RubyLikeInt implements Comparable<RubyLikeInt> {
  private final int small;
  private final BigInteger big;
  
  private RubyLikeInt() {
    this.small = 0;
    this.big = null;
    throw new AssertionError();
  }
  public static RubyLikeInt small(int small) {
    var integer = RubyLikeInt.default;
    return __WithField(integer.small, small);
  }
  public static RubyLikeInt big(BigInteger big) {
    var integer = RubyLikeInt.default;
    return __WithField(integer.big, big);
  }
  
  public int extractSmallValue() {  // only for testing
    return small;
  }
  
  @Override
  public String toString() {
    return (big == null)? "" + small: big.toString();
  }
  
  @Override
  public int compareTo(RubyLikeInt integer) {
    if (big == null && integer.big == null) {
      return Integer.compare(small, integer.small);
    }
    return bigCompareTo(this, integer);
  }
  private static int bigCompareTo(RubyLikeInt left, RubyLikeInt right) {
    return ((left.big == null)? valueOf(left.small): left.big).compareTo(
        (right.big == null)? valueOf(right.small): right.big);
  }
  
  public boolean even() {
    return (big == null)? small % 2 == 0: evenBig(this);
  }
  private static boolean evenBig(RubyLikeInt integer) {
    return integer.big.intValue() % 2 == 0;
  }
  public boolean odd() {
    return (big == null)? small % 2 == 1: oddBig(this);
  }
  private static boolean oddBig(RubyLikeInt integer) {
    return integer.big.intValue() % 2 == 1;
  }
  
  public RubyLikeInt next() {
    return pred();
  }
  
  public RubyLikeInt pred() {
    if (big == null) {
      try {
        return small(decrementExact(small));
      } catch(@SuppressWarnings("unused") ArithmeticException e) {
        return predOverflow(this);
      }
    }
    return predBig(this);
  }
  private static RubyLikeInt predOverflow(RubyLikeInt integer) {
    return big(valueOf(integer.small).subtract(BigInteger.ONE));
  }
  private static RubyLikeInt predBig(RubyLikeInt integer) {
    return big(integer.big.subtract(BigInteger.ONE));
  }
  
  public RubyLikeInt succ() {
    if (big == null) {
      try {
        return small(incrementExact(small));
      } catch(@SuppressWarnings("unused") ArithmeticException e) {
        return succOverflow(this);
      }
    }
    return succBig(this);
  }
  private static RubyLikeInt succOverflow(RubyLikeInt integer) {
    return big(valueOf(integer.small).add(BigInteger.ONE));
  }
  private static RubyLikeInt succBig(RubyLikeInt integer) {
    return big(integer.big.add(BigInteger.ONE));
  }
  
  public RubyLikeInt negate() {
    if (big == null) {
      try {
        return small(negateExact(small));
      } catch(@SuppressWarnings("unused") ArithmeticException e) {
        return negateOverflow(this);
      }
    }
    return negateBig(this);
  }
  private static RubyLikeInt negateOverflow(RubyLikeInt integer) {
    return big(valueOf(integer.small).negate());
  }
  private static RubyLikeInt negateBig(RubyLikeInt integer) {
    return big(integer.big.negate());
  }
  
  public RubyLikeInt add(RubyLikeInt integer) {
    if (big == null && integer.big == null) {
      try {
        return small(addExact(small, integer.small));
      } catch(@SuppressWarnings("unused") ArithmeticException e) {
        // empty
      }
    }
    return addFallback(this, integer);
  }
  private static RubyLikeInt addFallback(RubyLikeInt left, RubyLikeInt right) {
    if (left.big == null && right.big == null) {
      return addOverflow(left, right);
    }
    return addBig(left, right);
  }
  private static RubyLikeInt addOverflow(RubyLikeInt left, RubyLikeInt right) {
    return big(valueOf(left.small).add(valueOf(right.small)));
  }
  private static RubyLikeInt addBig(RubyLikeInt left, RubyLikeInt right) {
    return big(((left.big == null)? valueOf(left.small): left.big).add(
        (right.big == null)? valueOf(right.small): right.big));
  }
  
  public RubyLikeInt subtract(RubyLikeInt integer) {
    if (big == null && integer.big == null) {
      try {
        return small(subtractExact(small, integer.small));
      } catch(@SuppressWarnings("unused") ArithmeticException e) {
        // empty
      }
    }
    return subtractFallback(this, integer);
  }
  private static RubyLikeInt subtractFallback(RubyLikeInt left, RubyLikeInt right) {
    if (left.big == null && right.big == null) {
      return subtractOverflow(left, right);
    }
    return subtractBig(left, right);
  }
  private static RubyLikeInt subtractOverflow(RubyLikeInt left, RubyLikeInt right) {
    return big(valueOf(left.small).subtract(valueOf(right.small)));
  }
  private static RubyLikeInt subtractBig(RubyLikeInt left, RubyLikeInt right) {
    return big(((left.big == null)? valueOf(left.small): left.big).subtract(
        (right.big == null)? valueOf(right.small): right.big));
  }
  
  public RubyLikeInt multiply(RubyLikeInt integer) {
    if (big == null && integer.big == null) {
      try {
        return small(multiplyExact(small, integer.small));
      } catch(@SuppressWarnings("unused") ArithmeticException e) {
        // empty
      }
    }
    return multiplyFallback(this, integer);
  }
  private static RubyLikeInt multiplyFallback(RubyLikeInt left, RubyLikeInt right) {
    if (left.big == null && right.big == null) {
      return multiplyOverflow(left, right);
    }
    return multiplyBig(left, right);
  }
  private static RubyLikeInt multiplyOverflow(RubyLikeInt left, RubyLikeInt right) {
    return big(valueOf(left.small).multiply(valueOf(right.small)));
  }
  private static RubyLikeInt multiplyBig(RubyLikeInt left, RubyLikeInt right) {
    return big(((left.big == null)? valueOf(left.small): left.big).multiply(
        (right.big == null)? valueOf(right.small): right.big));
  }
  
  public RubyLikeInt divide(RubyLikeInt integer) {
    if (big == null && integer.big == null) {
      return small(small / integer.small);
    }
    return divideBig(this, integer);
  }
  private static RubyLikeInt divideBig(RubyLikeInt left, RubyLikeInt right) {
    return big(((left.big == null)? valueOf(left.small): left.big).divide(
        (right.big == null)? valueOf(right.small): right.big));
  }
  
  public void downto(RubyLikeInt limit, Consumer<? super RubyLikeInt> consumer) {
    for(var i = this; i.compareTo(limit) >= 0; i = i.pred()) {
      consumer.accept(i);
    }
  }
  
  public void upto(RubyLikeInt limit, Consumer<? super RubyLikeInt> consumer) {
    for(var i = this; i.compareTo(limit) <= 0; i = i.succ()) {
      consumer.accept(i);
    }
  }
  
  public void times(Consumer<? super RubyLikeInt> consumer) {
    small(0).upto(this, consumer);
  }
  
  public <T> T rangeReduce(RubyLikeInt to, T initial, BiFunction<? super T, ? super RubyLikeInt, ? extends T> reducer) {
    var current = initial;
    for(var i = this; i.compareTo(to) < 0; i = i.succ()) {
      current = reducer.apply(current, i);
    }
    return current;
  }
}
