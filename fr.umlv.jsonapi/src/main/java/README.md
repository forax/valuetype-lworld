# Proposal implementation for a Light-Weight JSON API (JEP 198)

This API is derived from the discussion we had on the OpenJDK mailing list about what should
be a light weight JSON API for Java (All the mistakes are mine).

The JEP 198 ask for supporting different usage modes:
- Low-level parsing token stream push/pull interface <br>
  (NOPE, i'm using Jackson for now)
- Mid-level push API, <br>
  [ObjectVisitor](fr/umlv/jsonapi/ObjectVisitor.java) and [ArrayVisitor](fr/umlv/jsonapi/ArrayVisitor.java) allow to see
  a JSON document as a series of events similar to StAX
- Mid-level pull java.util.stream,<br>
  [JsonReader.stream(Reader, ArrayVisitor)](fr/umlv/jsonapi/JsonReader.java)
  allows to use a stream as input to generate JSON events
- Immutable hierarchy of values (tree) constructed from JSON data stream,<br>
  [ObjectBuilder](fr/umlv/jsonapi/builder/ObjectBuilder.java) and [ArrayBuilder](fr/umlv/jsonapi/builder/ArrayBuilder.java)
  allows to create mutable and immutable java.util.List/java.util.Map,
  see {@link fr.umlv.jsonapi.builder.BuilderConfig} for more details.
- Tree constructed using Builder-style API,<br>
  builders have several constructions methods among
  [ObjectBuilder.add(String, Object)](fr/umlv/jsonapi/builder/ObjectBuilder.java),
  [ObjectBuilder#withObject(String, Consumer)](fr/umlv/jsonapi/builder/ObjectBuilder.java),
  [ObjectBuilder#withArray(String, Consumer)](fr/umlv/jsonapi/builder/ObjectBuilder.java)
  and [ArrayBuilder#add(java.lang.Object)](fr/umlv/jsonapi/builder/ArrayBuilder.java)
- Tree transformer API. A new tree is result,<br>
  JSON data stream output from trees,<br>
  builders methods [ObjectBuilder#replay(ObjectVisitor)](fr/umlv/jsonapi/builder/ObjectBuilder.java)
  and [ObjectBuilder#replay(ObjectVisitor)](fr/umlv/jsonapi/builder/ObjectBuilder.java)
  generates JSON events from the values of a builder.
- Generator style API for JSON data stream output and for JSON "literals",
  [JsonPrinter](fr/umlv/jsonapi/JsonPrinter.java) and [JsonWriter](fr/umlv/jsonapi/JsonWriter.java)
  generate JSON text fragment from JSON events.

Moreover this API also support a high level API, the [Binder](fr/umlv/jsonapi/bind/Binder.java)
- High-level API to encode any Java objects to JSON in a controlled manner,<br>
  [Binder#write(Writer, Object)](fr/umlv/jsonapi/bind/Binder.java).
- High-level API to decode to a tree of Java objects from a JSON derived from the
  [static type information](fr/umlv/jsonapi/bind/Spec.java) of the class declarations,<br>
  [Binder#read(Reader, Class)](fr/umlv/jsonapi/bind/Binder.java)
  and its stream version [Binder#stream(java.io.Reader, Class)](fr/umlv/jsonapi/bind/Binder.java)


