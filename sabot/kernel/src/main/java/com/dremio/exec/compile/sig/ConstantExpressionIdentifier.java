/*
 * Copyright (C) 2017 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.compile.sig;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.dremio.common.expression.BooleanOperator;
import com.dremio.common.expression.CastExpression;
import com.dremio.common.expression.ConvertExpression;
import com.dremio.common.expression.FunctionCall;
import com.dremio.common.expression.FunctionHolderExpression;
import com.dremio.common.expression.IfExpression;
import com.dremio.common.expression.LogicalExpression;
import com.dremio.common.expression.NullExpression;
import com.dremio.common.expression.SchemaPath;
import com.dremio.common.expression.TypedNullConstant;
import com.dremio.common.expression.ValueExpressions;
import com.dremio.common.expression.ValueExpressions.BooleanExpression;
import com.dremio.common.expression.ValueExpressions.DateExpression;
import com.dremio.common.expression.ValueExpressions.DecimalExpression;
import com.dremio.common.expression.ValueExpressions.DoubleExpression;
import com.dremio.common.expression.ValueExpressions.IntervalDayExpression;
import com.dremio.common.expression.ValueExpressions.IntervalYearExpression;
import com.dremio.common.expression.ValueExpressions.LongExpression;
import com.dremio.common.expression.ValueExpressions.QuotedString;
import com.dremio.common.expression.ValueExpressions.TimeExpression;
import com.dremio.common.expression.ValueExpressions.TimeStampExpression;
import com.dremio.common.expression.visitors.ExprVisitor;
import com.dremio.exec.expr.fn.ComplexWriterFunctionHolder;
import com.google.common.collect.Lists;

public class ConstantExpressionIdentifier implements ExprVisitor<Boolean, IdentityHashMap<LogicalExpression, Object>, RuntimeException>{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConstantExpressionIdentifier.class);

  private ConstantExpressionIdentifier(){}

  /**
   * Get a list of expressions that mark boundaries into a constant space.
   * @param e
   * @return
   */
  public static Set<LogicalExpression> getConstantExpressionSet(LogicalExpression e){
    IdentityHashMap<LogicalExpression, Object> map = new IdentityHashMap<>();
    ConstantExpressionIdentifier visitor = new ConstantExpressionIdentifier();


    if(e.accept(visitor, map) && map.isEmpty()){
      // if we receive a constant value here but the map is empty, this means the entire tree is a constant.
      // note, we can't use a singleton collection here because we need an identity set.
      map.put(e, true);
      return map.keySet();
    }else if(map.isEmpty()){
      // so we don't continue to carry around a map, we let it go here and simply return an empty set.
      return Collections.emptySet();
    }else{
      return map.keySet();
    }
  }

  private boolean checkChildren(LogicalExpression e, IdentityHashMap<LogicalExpression, Object> value, boolean transmitsConstant){
    List<LogicalExpression> constants = Lists.newLinkedList();
    boolean constant = true;

    for(LogicalExpression child : e){
      if(child.accept(this, value)){
        constants.add(child);
      }else{
        constant = false;
      }
    }

    // if one or more clauses isn't constant, this isn't constant.  this also isn't a constant if it operates on a set.
    if(!constant || !transmitsConstant){
      for(LogicalExpression c: constants){
        value.put(c, true);
      }
    }
    return constant && transmitsConstant;
  }

  @Override
  public Boolean visitFunctionCall(FunctionCall call, IdentityHashMap<LogicalExpression, Object> value){
    throw new UnsupportedOperationException("FunctionCall is not expected here. "+
      "It should have been converted to FunctionHolderExpression in materialization");
  }

  @Override
  public Boolean visitFunctionHolderExpression(FunctionHolderExpression holder, IdentityHashMap<LogicalExpression, Object> value) throws RuntimeException {
    return checkChildren(holder, value,
      !holder.isAggregating()
        && !holder.isRandom()
        && !(holder.getHolder() instanceof ComplexWriterFunctionHolder));
   }

  @Override
  public Boolean visitBooleanOperator(BooleanOperator op, IdentityHashMap<LogicalExpression, Object> value) throws RuntimeException {
    return checkChildren(op, value, true);
   }

  @Override
  public Boolean visitIfExpression(IfExpression ifExpr, IdentityHashMap<LogicalExpression, Object> value){
    return checkChildren(ifExpr, value, true);
  }

  @Override
  public Boolean visitSchemaPath(SchemaPath path, IdentityHashMap<LogicalExpression, Object> value){
    return false;
  }

  @Override
  public Boolean visitIntConstant(ValueExpressions.IntExpression intExpr, IdentityHashMap<LogicalExpression, Object> value) throws RuntimeException {
    return true;
  }

  @Override
  public Boolean visitFloatConstant(ValueExpressions.FloatExpression fExpr, IdentityHashMap<LogicalExpression, Object> value) throws RuntimeException {
    return true;
  }

  @Override
  public Boolean visitLongConstant(LongExpression intExpr, IdentityHashMap<LogicalExpression, Object> value){
    return true;
  }

  @Override
  public Boolean visitDateConstant(DateExpression intExpr, IdentityHashMap<LogicalExpression, Object> value){
    return true;
  }

  @Override
  public Boolean visitDecimalConstant(DecimalExpression decExpr, IdentityHashMap<LogicalExpression, Object> value){
    return true;
  }

  @Override
  public Boolean visitTimeConstant(TimeExpression intExpr, IdentityHashMap<LogicalExpression, Object> value){
    return true;
  }

  @Override
  public Boolean visitIntervalYearConstant(IntervalYearExpression intExpr, IdentityHashMap<LogicalExpression, Object> value){
    return true;
  }

  @Override
  public Boolean visitIntervalDayConstant(IntervalDayExpression intExpr, IdentityHashMap<LogicalExpression, Object> value){
    return true;
  }

  @Override
  public Boolean visitTimeStampConstant(TimeStampExpression intExpr, IdentityHashMap<LogicalExpression, Object> value){
    return true;
  }

  @Override
  public Boolean visitDoubleConstant(DoubleExpression dExpr, IdentityHashMap<LogicalExpression, Object> value){
    return true;
  }

  @Override
  public Boolean visitBooleanConstant(BooleanExpression e, IdentityHashMap<LogicalExpression, Object> value){
    return true;
  }

  @Override
  public Boolean visitQuotedStringConstant(QuotedString e, IdentityHashMap<LogicalExpression, Object> value){
    return true;
  }

  @Override
  public Boolean visitCastExpression(CastExpression e, IdentityHashMap<LogicalExpression, Object> value)
      throws RuntimeException {
    return e.getInput().accept(this, value);
  }

  @Override
  public Boolean visitUnknown(LogicalExpression e, IdentityHashMap<LogicalExpression, Object> value){
    return checkChildren(e, value, false);
  }

  @Override
  public Boolean visitNullConstant(TypedNullConstant e, IdentityHashMap<LogicalExpression, Object> value) throws RuntimeException {
    return true;
  }

  @Override
  public Boolean visitNullExpression(NullExpression e, IdentityHashMap<LogicalExpression, Object> value) throws RuntimeException {
    return true;
  }

  @Override
  public Boolean visitConvertExpression(ConvertExpression e,
      IdentityHashMap<LogicalExpression, Object> value) throws RuntimeException {
    return e.getInput().accept(this, value);
  }
}
