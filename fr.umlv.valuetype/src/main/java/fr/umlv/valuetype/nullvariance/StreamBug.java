package fr.umlv.valuetype.nullvariance;

import java.util.stream.IntStream;
/*
public @__inline__ class StreamBug {
  final int value;
  
  public StreamBug(int value) {
    this.value = value;
  }
  
  public static void main(String[] args) {
    //var bug = new StreamBug?(7);
    
    IntStream.range(0, 10).mapToObj(StreamBug?::new).forEach(System.out::println);
  }
}*/
