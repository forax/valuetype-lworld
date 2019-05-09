package fr.umlv.valuetype.nullvariance;

import java.util.function.Consumer;
/*
public @__inline__ class CaptureBug {
  final int value;
  
  public CaptureBug(int value) {
    this.value = value;
  }
  
  private static void accept(Consumer<? super CaptureBug?> consumer) {
    consumer.accept(new CaptureBug(3));
  }
  
  public static void main(String[] args) {
    accept(value -> System.out.println(value));
  }
}*/
