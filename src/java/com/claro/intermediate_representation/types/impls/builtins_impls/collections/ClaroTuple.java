package com.claro.intermediate_representation.types.impls.builtins_impls.collections;

import com.claro.intermediate_representation.types.Type;

// TODO(steving) Make this into an AutoValue class.

/**
 * NOTE TO FUTURE OVER-ZEALOUS-JASON...You can't genericize this class (at least not while backing storage with a Java
 * array), so don't bother going down that route.
 */
public class ClaroTuple implements Collection {
  private final Type claroType;
  // We store them in an array of object references.. this is lame because it's not contiguous memory but there's just
  // not a better option in Java.
  private final Object[] values;

  public ClaroTuple(Type claroType, Object... values) {
    this.claroType = claroType;
    this.values = values;
  }

  public void set(int index, Object val) {
    // Bounds for Tuple reassignment were already checked at compile time.
    this.values[index] = val;
  }

  @SuppressWarnings("unchecked") // The whole point is that we're already checking this.
  public <T> T getElement(int i) {
    return (T) this.values[i];
  }

  public int length() {
    return this.values.length;
  }

  @Override
  public String toString() {
    StringBuilder res = new StringBuilder();
    res.append("(");
    for (int i = 0; i < this.length() - 1; i++) {
      res.append(this.values[i].toString());
      res.append(", ");
    }
    res.append(this.getElement(this.length() - 1).toString());
    res.append(")");
    return res.toString();
  }

  @Override
  public Type getClaroType() {
    return claroType;
  }

  // TODO(steving) I need to be able to do a simple equality check on tuples in Claro code. This would involve also
  //  figuring out the appropriate way to override the hashcode() method... leaving that for later.
//  @Override
//  public boolean equals(Object other) {
//    if (!(other instanceof ClaroTuple)) {
//      return false;
//    }
//    ClaroTuple otherTuple = (ClaroTuple) other;
//    if (this.length() != otherTuple.length()) {
//      return false;
//    }
//    for (Object value : this.values) {
//      if (!value.equals(otherTuple)) {
//        return false;
//      }
//    }
//    return true;
//  }
}
