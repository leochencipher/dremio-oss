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
package com.dremio.exec.store.parquet.r3;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.junit.Test;

import com.dremio.exec.planner.logical.partition.FindSimpleFilters;
import com.dremio.exec.planner.logical.partition.FindSimpleFilters.StateHolder;
import com.google.common.collect.ImmutableList;

public class TestFilterFinder {

  private RelDataTypeFactory factory = new JavaTypeFactoryImpl();
  private RexBuilder builder = new RexBuilder(factory);

  @Test
  public void simpleLiteralEquality(){

    final RexNode node = builder.makeCall(SqlStdOperatorTable.EQUALS,
        builder.makeBigintLiteral(BigDecimal.ONE),
        builder.makeInputRef(factory.createSqlType(SqlTypeName.BIGINT), 0)
        );

    FindSimpleFilters finder = new FindSimpleFilters(builder);
    StateHolder holder = node.accept(finder);
    ImmutableList<RexCall> conditions = holder.getConditions();

    assertEquals(1, conditions.size());
    assertFalse(holder.hasRemainingExpression());

  }

  @Test
  public void simpleLiteralReverseEquality(){

    final RexNode node = builder.makeCall(SqlStdOperatorTable.EQUALS,
        builder.makeInputRef(factory.createSqlType(SqlTypeName.BIGINT), 0),
        builder.makeBigintLiteral(BigDecimal.ONE)
        );

    FindSimpleFilters finder = new FindSimpleFilters(builder);
    StateHolder holder = node.accept(finder);
    ImmutableList<RexCall> conditions = holder.getConditions();

    assertEquals(1, conditions.size());
    assertFalse(holder.hasRemainingExpression());
  }

  @Test
  public void typeMismatchFailure(){
    final RexNode node = builder.makeCall(SqlStdOperatorTable.EQUALS,
        builder.makeInputRef(factory.createSqlType(SqlTypeName.INTEGER), 0),
        builder.makeBigintLiteral(BigDecimal.ONE)
        );

    FindSimpleFilters finder = new FindSimpleFilters(builder, true);
    StateHolder holder = node.accept(finder);
    ImmutableList<RexCall> conditions = holder.getConditions();

    assertEquals(0, conditions.size());
    assertTrue(holder.hasRemainingExpression());
  }

  @Test
  public void typeMismatchSuccess(){
    final RexNode node = builder.makeCall(SqlStdOperatorTable.EQUALS,
        builder.makeInputRef(factory.createSqlType(SqlTypeName.INTEGER), 0),
        builder.makeBigintLiteral(BigDecimal.ONE)
        );

    FindSimpleFilters finder = new FindSimpleFilters(builder, false);
    StateHolder holder = node.accept(finder);
    ImmutableList<RexCall> conditions = holder.getConditions();

    assertEquals(1, conditions.size());
    assertFalse(holder.hasRemainingExpression());
  }

  @Test
  public void nullEquality(){

    final RexNode node = builder.makeCall(SqlStdOperatorTable.EQUALS,
        builder.makeInputRef(factory.createSqlType(SqlTypeName.BIGINT), 0),
        builder.makeNullLiteral(SqlTypeName.BIGINT)
        );

    FindSimpleFilters finder = new FindSimpleFilters(builder);
    StateHolder holder = node.accept(finder);
    ImmutableList<RexCall> conditions = holder.getConditions();

    assertEquals(0, conditions.size());
    assertTrue(holder.hasRemainingExpression());
  }

  @Test
  public void halfTree(){
    final RexNode node =
        builder.makeCall(SqlStdOperatorTable.AND,
        builder.makeCall(SqlStdOperatorTable.EQUALS,
            builder.makeInputRef(factory.createSqlType(SqlTypeName.BIGINT), 0),
            builder.makeBigintLiteral(BigDecimal.ONE)
            ),
        builder.makeApproxLiteral(BigDecimal.ONE)
        );

    FindSimpleFilters finder = new FindSimpleFilters(builder);
    StateHolder holder = node.accept(finder);
    ImmutableList<RexCall> conditions = holder.getConditions();

    assertEquals(1, conditions.size());
    assertEquals(SqlKind.EQUALS, conditions.get(0).getKind());
    assertEquals(SqlKind.LITERAL, holder.getNode().getKind());
    assertTrue(holder.hasRemainingExpression());
  }


  @Test
  public void noOnOr(){
    final RexNode node =
        builder.makeCall(SqlStdOperatorTable.OR,
        builder.makeCall(SqlStdOperatorTable.EQUALS,
            builder.makeInputRef(factory.createSqlType(SqlTypeName.BIGINT), 0),
            builder.makeBigintLiteral(BigDecimal.ONE)
            ),
        builder.makeApproxLiteral(BigDecimal.ONE)
        );

    FindSimpleFilters finder = new FindSimpleFilters(builder);
    StateHolder holder = node.accept(finder);
    ImmutableList<RexCall> conditions = holder.getConditions();

    assertEquals(0, conditions.size());
    assertTrue(holder.hasRemainingExpression());
  }

  @Test
  public void doubleAnd(){
    final RexNode node =
        builder.makeCall(SqlStdOperatorTable.AND,
        builder.makeCall(SqlStdOperatorTable.EQUALS,
            builder.makeInputRef(factory.createSqlType(SqlTypeName.BIGINT), 0),
            builder.makeBigintLiteral(BigDecimal.ONE)
            ),
        builder.makeCall(SqlStdOperatorTable.EQUALS,
            builder.makeInputRef(factory.createSqlType(SqlTypeName.BIGINT), 0),
            builder.makeBigintLiteral(BigDecimal.ONE)
            )
        );

    FindSimpleFilters finder = new FindSimpleFilters(builder);
    StateHolder holder = node.accept(finder);
    ImmutableList<RexCall> conditions = holder.getConditions();

    assertEquals(2, conditions.size());
    assertEquals(SqlKind.EQUALS, conditions.get(0).getKind());
    assertEquals(SqlKind.EQUALS, conditions.get(1).getKind());
    assertFalse(holder.hasRemainingExpression());
  }
}
