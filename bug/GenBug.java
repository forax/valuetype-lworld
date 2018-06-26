public final __ByValue class GenBug<E> {
  private final boolean value;
  
  private GenBug() {
    value = false;
    throw new AssertionError();
  }

  public static <E> GenBug<E> create() {
    GenBug<E> bug = __MakeDefault GenBug<E>();
    bug = __WithField(bug.value, true);
    return bug;
  }
}
