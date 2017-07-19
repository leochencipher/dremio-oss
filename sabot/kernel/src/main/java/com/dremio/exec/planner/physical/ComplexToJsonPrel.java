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
package com.dremio.exec.planner.physical;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.SingleRel;

import com.dremio.exec.physical.base.PhysicalOperator;
import com.dremio.exec.physical.config.ComplexToJson;
import com.dremio.exec.planner.physical.visitor.PrelVisitor;
import com.dremio.exec.record.BatchSchema.SelectionVectorMode;

import org.apache.calcite.plan.RelTraitSet;

public class ComplexToJsonPrel extends SingleRel implements Prel, CustomPrel {

  public ComplexToJsonPrel(Prel phyRelNode) {
    super(phyRelNode.getCluster(), phyRelNode.getTraitSet(), phyRelNode);
  }

  @Override
  public final RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new ComplexToJsonPrel((Prel) sole(inputs));
  }

  @Override
  public Prel getOriginPrel() {
    return ((Prel) getInput());
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    ComplexToJson p = new ComplexToJson(((Prel) getInput()).getPhysicalOperator(creator));
    return creator.addMetadata(this, p);
  }

  @Override
  public Iterator<Prel> iterator() {
    return PrelUtil.iter(getInput());
  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.DEFAULT;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitPrel(this, value);
  }

  @Override
  public boolean needsFinalColumnReordering() {
    return true;
  }

}
