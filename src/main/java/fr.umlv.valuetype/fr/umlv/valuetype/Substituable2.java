package fr.umlv.valuetype;

public class Substituable2 {
  /*
  interface Strategy<T> {
    Optional<T> apply(String text);
    
    public default Strategy<T> or(Strategy<T> strategy) {
      return new value Strategy<>() {
        public Optional<T> apply(String text) {
          return strategy.apply(text).or(() -> Strategy.this.apply(text));
        }
      };
    }
    
    public static <T> Strategy<T> empty() {
      return new value Strategy<>() {
        boolean empty = false;
        public Optional<T> apply(String text) {
          return Optional.empty(); 
        }
      };
    }
    
    public static Strategy<String> isEquals(String value) {
      return new value Strategy<>() {
        public Optional<String> apply(String text) {
          return Optional.of(value).filter(val -> val.equals(text));
        }
      };
    }
  }
  
  static Strategy<String> create(List<String> list) {
    return list.stream().reduce(Strategy.empty(), (acc, s) -> acc.or(Strategy.isEquals(s)), (_1, _2) -> { throw null; });
  }
  
  
  public static void main(String[] args) {
    var strategy = create(IntStream.range(0, 1_000_000).mapToObj(Integer::toString).collect(toList()));
    
    System.out.println(strategy.apply("999990").orElseThrow());
    
    //System.out.println(strategy == strategy);
    System.out.println(ValueBootstrapMethods.isSubstitutable(strategy, strategy.or(Strategy.isEquals("foo"))));
  }*/
  
  interface Fun {
    String toString();
    
    static Fun of(int value, Object next) {
      return new inline Fun() {
        public String toString() {
          return value + " -> " + next;
        }
      };
    }
  }
  
  public static void main(String[] args) {
  	System.out.println(Fun.of(1, null) == Fun.of(2, null));
    //System.out.println(java.lang.invoke.ValueBootstrapMethods.isSubstitutable(Fun.of(1, null), Fun.of(2, null)));
  }
}
