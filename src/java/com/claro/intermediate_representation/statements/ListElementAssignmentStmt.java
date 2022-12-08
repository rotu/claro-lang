package com.claro.intermediate_representation.statements;

import com.claro.compiler_backends.interpreted.ScopedHeap;
import com.claro.intermediate_representation.expressions.CollectionSubscriptExpr;
import com.claro.intermediate_representation.expressions.Expr;
import com.claro.intermediate_representation.expressions.term.IdentifierReferenceTerm;
import com.claro.intermediate_representation.expressions.term.IntegerTerm;
import com.claro.intermediate_representation.types.BaseType;
import com.claro.intermediate_representation.types.ClaroTypeException;
import com.claro.intermediate_representation.types.Type;
import com.claro.intermediate_representation.types.Types;
import com.claro.intermediate_representation.types.impls.builtins_impls.collections.ClaroList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class ListElementAssignmentStmt extends Stmt {

  private static final ImmutableSet<BaseType> SUPPORTED_EXPR_BASE_TYPES =
      ImmutableSet.of(
          BaseType.LIST,
          BaseType.TUPLE,
          BaseType.MAP
      );

  public ListElementAssignmentStmt(CollectionSubscriptExpr collectionSubscriptExpr, Expr e) {
    super(
        ImmutableList.of(
            /*listExpr=*/
            collectionSubscriptExpr.getChildren().get(0),
            /*subscriptExpr=*/
            collectionSubscriptExpr.getChildren().get(1),
            /*assignedValueExpr=*/
            e
        )
    );
  }

  @Override
  public void assertExpectedExprTypes(ScopedHeap scopedHeap) throws ClaroTypeException {
    // First thing first, we need to actually validate that we're correctly referencing a collection type.
    Expr listExpr = (IdentifierReferenceTerm) this.getChildren().get(0);
    Type listExprType = listExpr.getValidatedExprType(scopedHeap);
    if (!SUPPORTED_EXPR_BASE_TYPES.contains(listExprType.baseType())) {
      // Make sure that this mismatch is logged on the offending Expr that was supposed to be a collection.
      listExpr.assertSupportedExprBaseType(scopedHeap, SUPPORTED_EXPR_BASE_TYPES);
      ((Expr) this.getChildren().get(1)).logTypeError(
          ClaroTypeException.forInvalidSubscriptForNonCollectionType(listExprType, SUPPORTED_EXPR_BASE_TYPES));

      // We can't do any more type checking of the rhs because the entire premise of this assignment statement is invalid.
      // However, just in case we need to mark things used, let's run validation on the RHS...not perfect but helpful.
      ((Expr) this.getChildren().get(2)).getValidatedExprType(scopedHeap);
      return;
    }

    // Type check the index and rhs exprs.
    if (listExprType.baseType().equals(BaseType.MAP)) {
      ((Expr) this.getChildren().get(1)).assertExpectedExprType(
          scopedHeap, listExprType.parameterizedTypeArgs().get(Types.MapType.PARAMETERIZED_TYPE_KEYS));
      ((Expr) this.getChildren().get(2)).assertExpectedExprType(
          scopedHeap,
          listExprType.parameterizedTypeArgs().get(Types.MapType.PARAMETERIZED_TYPE_VALUES)
      );
    } else if (listExprType.baseType().equals(BaseType.TUPLE)) {
      Expr subscriptExpr = (Expr) this.getChildren().get(1);
      if (!(subscriptExpr instanceof IntegerTerm)) { // Tuples can only be subscripted for re-assignment using a literal.
        subscriptExpr.logTypeError(ClaroTypeException.forTupleIndexNonLiteralForAssignment());
      } else {
        // We know we have a compile-time constant integer literal, so I'm going to bounds check for the user now.
        int subscriptValue = ((IntegerTerm) subscriptExpr).value;
        ImmutableList<Type> tupleTypes = ((Types.TupleType) listExprType).getValueTypes();
        if (subscriptValue >= tupleTypes.size() || subscriptValue < 0) {
          subscriptExpr.logTypeError(
              ClaroTypeException.forTupleIndexOutOfBounds(listExprType, tupleTypes.size(), subscriptValue));
        } else {
          ((Expr) this.getChildren().get(2)).assertExpectedExprType(
              scopedHeap,
              tupleTypes.get(subscriptValue)
          );
        }
      }
    } else {
      ((Expr) this.getChildren().get(1)).assertExpectedExprType(scopedHeap, Types.INTEGER);
      ((Expr) this.getChildren().get(2)).assertExpectedExprType(
          scopedHeap,
          ((Types.Collection) listExprType).getElementType()
      );
    }
  }

  @Override
  public GeneratedJavaSource generateJavaSourceOutput(ScopedHeap scopedHeap) {
    GeneratedJavaSource genJavaSource0 = this.getChildren().get(0).generateJavaSourceOutput(scopedHeap);
    GeneratedJavaSource genJavaSource1 = this.getChildren().get(1).generateJavaSourceOutput(scopedHeap);
    GeneratedJavaSource genJavaSource2 = this.getChildren().get(2).generateJavaSourceOutput(scopedHeap);

    GeneratedJavaSource resGenJavaSource = GeneratedJavaSource.forJavaSourceBody(
        new StringBuilder(
            String.format(
                "%s.set(%s, %s);\n",
                genJavaSource0.javaSourceBody().toString(),
                genJavaSource1.javaSourceBody().toString(),
                genJavaSource2.javaSourceBody().toString()
            )
        )
    );
    // We've already consumed javaSourceBodyStmt, it's safe to clear.
    genJavaSource0.javaSourceBody().setLength(0);
    genJavaSource1.javaSourceBody().setLength(0);
    genJavaSource2.javaSourceBody().setLength(0);

    return resGenJavaSource.createMerged(genJavaSource0).createMerged(genJavaSource1).createMerged(genJavaSource2);
  }

  // Type info is lost on generateInterpretedOutput, but we know Claro only allows subscript assignment on lists.
  @SuppressWarnings("unchecked")
  @Override
  public Object generateInterpretedOutput(ScopedHeap scopedHeap) {
    // Put the computed value of this specified List element directly into ArrayList in the heap.
    ((ClaroList<Object>) this.getChildren().get(0).generateInterpretedOutput(scopedHeap))
        .set(
            (int) this.getChildren().get(1).generateInterpretedOutput(scopedHeap),
            this.getChildren().get(2).generateInterpretedOutput(scopedHeap)
        );
    return null;
  }
}
