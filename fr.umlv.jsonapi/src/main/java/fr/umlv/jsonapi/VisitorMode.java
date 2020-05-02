package fr.umlv.jsonapi;

/**
 * The {@link ObjectVisitor} (and respectively the {@link ArrayVisitor}) use the same API but
 * the semantics is slightly different if the JSON values comes from the
 * {@link JsonReader JSON reader} (push mode) or if the JSON values are pulled to be inserted
 * in a stream (pull mode).
 *
 * Moreover, the {@link ArrayVisitor} has a special third mode (pull inside mode) where
 * the stream is only available inside a {@link ArrayVisitor#visitStream(java.util.stream.Stream)
 * specific method} inside the array visit.
 *
 * The code of the {@link JsonReader JSON reader} works differently depending on the visitor mode
 * and some methods throws an {@link IllegalStateException} if they are called with a visitor
 * not in the right mode.
 */
public enum VisitorMode {
  PUSH, PULL, PULL_INSIDE
}
