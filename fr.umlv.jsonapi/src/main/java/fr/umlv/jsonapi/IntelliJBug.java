package fr.umlv.jsonapi;

public class IntelliJBug {
  interface Foo {
    private void bar() {
      new Runnable() {
        @Override
        public void run() {
          System.out.println(Foo.this);
        }
      };
    }
  }
}
