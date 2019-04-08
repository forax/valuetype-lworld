package fr.umlv.valuetype;

import java.util.Random;
import java.util.stream.IntStream;

@__value__
public /*value*/ class GuessANumber {
  private final int value;
  
  public GuessANumber(int value) {
    this.value = value;
  }
  
  public enum Response { LOWER, GREATER, FOUND }
  
  public Response guess(int guess) {
    if (value < guess) {
      return Response.LOWER;
    }
    if (value > guess) {
      return Response.GREATER;
    }
    return Response.FOUND;
  }
  
  public static GuessANumber random(int seed) {
    return new GuessANumber(new Random(seed).nextInt(1024));
  }
  
  public static void main(String[] args) {
    var number = GuessANumber.random(0);
    System.out.println(IntStream.range(0, 1024).filter(n -> new GuessANumber(n) == number).findFirst());
  }
}
