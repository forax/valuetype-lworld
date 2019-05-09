package fr.umlv.valuetype.nullvariance;

import java.util.Objects;
import java.util.function.Function;

import fr.umlv.valuetype.Option;
/*
@__inline__
public class Holder<E> {
  final E element;
  
  public Holder(E element) {
    this.element = element;
  }

  public <R> Holder<R> flatMap(Function<? super E, ? extends Holder?<R>> mapper) {
    return (Holder<R>)mapper.apply(element);
  }
  
  public <R> Holder<R> map(Function<? super E, ? extends R> mapper) {
    return flatMap(value -> new Holder<R>(mapper.apply(value)));
  }
  
  public static void main(String[] args) {
    Holder<String> issue = new Holder<>("hello");
    Holder<Integer> issue2 = issue.map(String::length);
    System.out.println(issue2.element);
  }
}*/


