/**
 * Proposal implementation for a Light-Weight JSON API (JEP 198)
 *
 * <p>This API is derived from the discussion we had on the OpenJDK mailing list about what should
 * be a light weight JSON API for Java All the mistakes are mine.
 *
 * <p>The JEP 198 ask for supporting different usage modes:
 *
 * <ul>
 *   <li>Low-level parsing token stream push/pull interface <br>
 *       (NOPE, i'm using Jackson for now)
 *   <li>Mid-level push API, <br>
 *       {@link fr.umlv.jsonapi.ObjectVisitor} and {@link fr.umlv.jsonapi.ArrayVisitor} allow to see
 *       a JSON document as a series of events similar to StAX
 *   <li>Mid-level pull java.util.stream,<br>
 *       {@link fr.umlv.jsonapi.JsonReader#stream(java.io.Reader, fr.umlv.jsonapi.ArrayVisitor)}
 *       allows to use a stream as input to generate JSON events
 *   <li>Immutable hierarchy of values (tree) constructed from JSON data stream,<br>
 *       {@link fr.umlv.jsonapi.builder.ObjectBuilder} and {@link
 *       fr.umlv.jsonapi.builder.ArrayBuilder} allows to create mutable and immutable
 *       java.util.List/java.util.Map, see {@link fr.umlv.jsonapi.builder.BuilderConfig} for more
 *       details.
 *   <li>Tree constructed using Builder-style API,<br>
 *       builders have several constructions methods among
 *       {@link fr.umlv.jsonapi.builder.ObjectBuilder#add(java.lang.String, java.lang.Object)},
 *       {@link fr.umlv.jsonapi.builder.ObjectBuilder#withObject(java.lang.String, java.util.function.Consumer)},
 *       {@link fr.umlv.jsonapi.builder.ObjectBuilder#withArray(java.lang.String, java.util.function.Consumer)}
 *       and {@link fr.umlv.jsonapi.builder.ArrayBuilder#add(java.lang.Object)}.
 *   <li>Tree transformer API. A new tree is result,<br>
 *       JSON data stream output from trees,<br>
 *       builders methods {@link fr.umlv.jsonapi.builder.ObjectBuilder#replay(fr.umlv.jsonapi.ObjectVisitor)}
 *       and {@link fr.umlv.jsonapi.builder.ObjectBuilder#replay(fr.umlv.jsonapi.ObjectVisitor)}
 *       generates JSON events from the values of a builder.
 *   <li>Generator style API for JSON data stream output and for JSON "literals",
 *       {@link fr.umlv.jsonapi.JsonPrinter} and {@link fr.umlv.jsonapi.JsonWriter}
 *       generate JSON text fragment from JSON events.
 * </ul>
 *
 * Moreover this API also support a high level API, the {@link fr.umlv.jsonapi.bind.Binder}
 * <ul>
 *   <li>High-level API to encode any Java objects to JSON in a controlled manner,<br>
 *       {@link fr.umlv.jsonapi.bind.Binder#write(java.io.Writer, java.lang.Object)}.
 *   <li>High-level API to decode to a tree of Java objects from a JSON derived from the
 *       {@link fr.umlv.jsonapi.bind.Spec static type information} of the class declarations,<br>
 *       {@link fr.umlv.jsonapi.bind.Binder#read(java.io.Reader, Class)}
 *       and its stream version
 *       {@link fr.umlv.jsonapi.bind.Binder#stream(java.io.Reader, Class)}.
 * </ul>
 */
package fr.umlv.jsonapi;
