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
package com.dremio.exec.store;

import java.util.List;

import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptSchema;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelDistribution;
import org.apache.calcite.rel.RelDistributionTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.util.ImmutableBitSet;

import com.google.common.collect.ImmutableList;
import com.google.common.base.Objects;

/**
 * Namespace table associated with a particular RelOptCluster.
 */
public class RelOptNamespaceTable implements RelOptTable {

  private final NamespaceTable table;
  private final RelOptCluster cluster;


  public RelOptNamespaceTable(TableMetadata dataset, RelOptCluster cluster) {
    this(new NamespaceTable(dataset), cluster);
  }

  public RelOptNamespaceTable(NamespaceTable table, RelOptCluster cluster) {
    super();
    this.table = table;
    this.cluster = cluster;
  }

  @Override
  public List<String> getQualifiedName() {
    return table.getDataset().getName().getPathComponents();
  }

  @Override
  public double getRowCount() {
    return table.getStatistic().getRowCount();
  }

  @Override
  public RelDataType getRowType() {
    return table.getRowType(cluster.getTypeFactory());
  }

  @Override
  public RelOptSchema getRelOptSchema() {
    return null;
  }

  @Override
  public RelNode toRel(ToRelContext context) {
    return table.toRel(context, this);
  }

  @Override
  public List<RelCollation> getCollationList() {
    return ImmutableList.of();
  }

  @Override
  public RelDistribution getDistribution() {
    return RelDistributionTraitDef.INSTANCE.getDefault();
 }

  @Override
  public boolean isKey(ImmutableBitSet columns) {
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T unwrap(Class<T> clazz) {
    if(clazz == NamespaceTable.class){
      return (T) table;
    }
    return null;
  }

  @Override
  public Expression getExpression(Class clazz) {
    throw new UnsupportedOperationException("Should never be called.");
  }

  @Override
  public RelOptTable extend(List<RelDataTypeField> extendedFields) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof RelOptNamespaceTable)) {
      return false;
    }
    RelOptNamespaceTable castOther = (RelOptNamespaceTable) other;
    return Objects.equal(table.getDataset().getName(), castOther.table.getDataset().getName()) && Objects.equal(cluster, castOther.cluster);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(table, cluster);
  }

}
