/**
 * This package provides two builders {@link fr.umlv.jsonapi.builder.ObjectBuilder} and {@link
 * fr.umlv.jsonapi.builder.ArrayBuilder} implementing respectively the interface {@link
 * fr.umlv.jsonapi.ObjectVisitor} and {@link fr.umlv.jsonapi.ArrayVisitor}.
 *
 * <p>Those builders can be used to collect all the JSON values, again respectively, in a {@link
 * java.util.Map Map&lt;String, Object&gt;} or a {@link java.util.List List&lt;Object&gt;}
 *
 * <p>By example, to get a map from a JSON text
 *
 * <pre>
 * String text = """
 *   {
 *     "foo": 3,
 *     "bar": "whizz"
 *   }
 *   """;
 * ObjectBuilder builder = new ObjectBuilder();
 * Map&lt;String, Object&gt; map = JsonReader.parse(text, builder);
 * </pre>
 *
 * <p>Or, to get an immutable list from a JSON text
 *
 * <pre>
 * String text = """
 *   [ "foo", 42, 66.6 ]
 *   """;
 * ArrayBuilder builder = BuilderConfig
 *         .defaults()
 *         .withTransformListOp(List::copyOf)
 *         .newArrayBuilder();
 * List&lt;Object&gt; list = JsonReader.parse(text, builder);
 * </pre>
 *
 * <p>Builders also provide an API to easily creates a tree of JSON values
 *
 * <pre>
 * ObjectBuilder builder = new ObjectBuilder()
 *     .add("id", 145)
 *     .withObject("info", b -> b
 *         .add("generator", 6))
 *     .withArray("data", b -> b
 *         .addAll(4, 7, 3, 78))
 *     .add("allowed", true);
 * </pre>
 *
 * <p>Builders can also replay a sequence of visits from the builder's values
 * By example, the code of the method {@code toString()} of a builder is
 * <pre>
 *   return builder.replay(new JsonPrinter()).toString();
 * </pre>
 */
package fr.umlv.jsonapi.builder;
