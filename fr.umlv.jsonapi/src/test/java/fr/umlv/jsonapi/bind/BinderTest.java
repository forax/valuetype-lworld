package fr.umlv.jsonapi.bind;

import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.umlv.jsonapi.bind.Binder.BindingException;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class BinderTest {
  @Test
  public void noDefaults() {
    var binder = Binder.noDefaults();
    assertNotNull(binder.spec(int.class));
    assertNotNull(binder.spec(String.class));
    assertThrows(BindingException.class, () -> binder.spec(URI.class));
  }

  @Test
  public void findSpec() {
    var binder = new Binder(lookup());
    record Point(int x, int y) { }
    var spec = binder.spec(Point.class);
    assertEquals("Point", spec.toString());
  }

  @Test
  public void specFinder() {
    var binder = new Binder(lookup());
    record Point(int x, int y) { }
    var finder = binder.specFinder();
    var spec = finder.findSpec(Point.class);
    assertEquals("Point", spec.orElseThrow().toString());
  }
}