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
package com.dremio.exec.physical.config;

import com.dremio.exec.expr.fn.FunctionLookupContext;
import com.dremio.exec.physical.base.AbstractSingle;
import com.dremio.exec.physical.base.PhysicalOperator;
import com.dremio.exec.physical.base.PhysicalVisitor;
import com.dremio.exec.proto.UserBitShared.CoreOperatorType;
import com.dremio.exec.record.BatchSchema;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("limit")
public class Limit extends AbstractSingle {
  private final Integer first;
  private final Integer last;

  @JsonCreator
  public Limit(@JsonProperty("child") PhysicalOperator child, @JsonProperty("first") Integer first, @JsonProperty("last") Integer last) {
    super(child);
    this.first = first;
    this.last = last;
  }

  public Integer getFirst() {
    return first;
  }

  public Integer getLast() {
    return last;
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new Limit(child, first, last);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitLimit(this, value);
  }

  @Override
  public BatchSchema getSchema(FunctionLookupContext context) {
    return child.getSchema(context);
  }

  @Override
  public int getOperatorType() {
    return CoreOperatorType.LIMIT_VALUE;
  }
}
