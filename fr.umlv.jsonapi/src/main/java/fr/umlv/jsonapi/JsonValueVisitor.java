package fr.umlv.jsonapi;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface JsonValueVisitor {
  void visitNull();
  void visitBoolean(boolean value);
  void visitInt(int value);
  void visitLong(long value);
  void visitDouble(double value);
  void visitString(String value);
  void visitBigInteger(BigInteger value);
  void visitBigDecimal(BigDecimal value);
}
