package fr.umlv.jsonapi;

import fr.umlv.jsonapi.filter.FilterObjectVisitor;
import fr.umlv.jsonapi.filter.RenamerObjectVisitor;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public interface ObjectVisitor {
  ObjectVisitor visitMemberObject(String name);
  ArrayVisitor visitMemberArray(String name);
  void visitMemberValue(String name, JsonValue value);
  Object visitEndObject();

  default ObjectVisitor mapName(UnaryOperator<String> renamer) {
    return new RenamerObjectVisitor(this, renamer);
  }

  default ObjectVisitor filterName(Predicate<? super String> predicate) {
    return new FilterObjectVisitor(this, predicate);
  }
}