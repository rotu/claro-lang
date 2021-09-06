package com.claro.examples.calculator_example.intermediate_representation.types.builtins_impls;

import com.claro.examples.calculator_example.intermediate_representation.types.Type;

// This interface simply marks an outermost boundary layer around Types built natively within Claro so that we can make
// certain assumptions about the types that we're dealing with in Claro. This is important because native-Java interop
// is a top-level priority for Claro, so some special handling may be needed to make that interop possible.
public interface ClaroTypeImplementation {
  public Type getClaroType();
}
