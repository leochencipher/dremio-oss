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
package com.dremio.exec.record;

import java.lang.reflect.Array;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.ValueVector;
import org.apache.arrow.vector.complex.AbstractMapVector;
import org.apache.arrow.vector.complex.FieldIdUtil2;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.util.CallBack;
import org.apache.commons.lang3.ArrayUtils;

import com.dremio.common.expression.SchemaPath;
import com.google.common.base.Preconditions;


public class HyperVectorWrapper<T extends ValueVector> implements VectorWrapper<T>{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HyperVectorWrapper.class);

  private T[] vectors;
  private Field field;
  private final boolean releasable;

  public HyperVectorWrapper(Field field, T[] v) {
    this(field, v, true);
  }

  public HyperVectorWrapper(Field f, T[] v, boolean releasable) {
    this.field = f;
    this.vectors = v;
    this.releasable = releasable;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<T> getVectorClass() {
    return (Class<T>) vectors.getClass().getComponentType();
  }

  @Override
  public Field getField() {
    return field;
  }

  @Override
  public T getValueVector() {
    throw new IllegalStateException("Tried to retrieve single vector on a hyper vector.");
  }

  @Override
  public T[] getValueVectors() {
    return vectors;
  }

  @Override
  public boolean isHyper() {
    return true;
  }

  @Override
  public void close(){
    clear();
  }

  @Override
  public void clear() {
    if (!releasable) {
      return;
    }
    for (T x : vectors) {
      x.clear();
    }
  }

  @Override
  public VectorWrapper<?> getChildWrapper(int[] ids) {
    if (ids.length == 1) {
      return this;
    }

    ValueVector[] vectors = new ValueVector[this.vectors.length];
    int index = 0;

    for (ValueVector v : this.vectors) {
      ValueVector vector = v;
      for (int i = 1; i < ids.length; i++) {
        final AbstractMapVector mapLike = AbstractMapVector.class.cast(vector);
        if (mapLike == null) {
          return null;
        }
        vector = mapLike.getChildByOrdinal(ids[i]);
      }
      vectors[index] = vector;
      index++;
    }
    return new HyperVectorWrapper<ValueVector>(vectors[0].getField(), vectors);
  }

  @Override
  public TypedFieldId getFieldIdIfMatches(int id, SchemaPath expectedPath) {
    return FieldIdUtil2.getFieldId(field, id, expectedPath, true);
  }

  @Override
  @SuppressWarnings("unchecked")
  public VectorWrapper<T> cloneAndTransfer(BufferAllocator allocator, CallBack callback) {
    if(vectors.length == 0){
      return new HyperVectorWrapper<T>(field, vectors, false);
    }else {
      T[] newVectors = (T[]) Array.newInstance(vectors[0].getClass(), vectors.length);
      for(int i =0; i < vectors.length; i++){
        newVectors[i] = (T) vectors[i].getTransferPair(vectors[i].getField().getName(), allocator, callback).getTo();
      }
      return new HyperVectorWrapper<T>(field, vectors, false);
    }
  }

  public static <T extends ValueVector> HyperVectorWrapper<T> create(Field f, T[] v, boolean releasable) {
    return new HyperVectorWrapper<T>(f, v, releasable);
  }

  @SuppressWarnings("unchecked")
  public void addVector(ValueVector v) {
    Preconditions.checkArgument(v.getClass() == this.getVectorClass(), String.format("Cannot add vector type %s to hypervector type %s for field %s",
      v.getClass(), this.getVectorClass(), v.getField()));
    vectors = (T[]) ArrayUtils.add(vectors, v);// TODO optimize this so not copying every time
  }

  @SuppressWarnings("unchecked")
  public void addVectors(ValueVector[] vv) {
    vectors = (T[]) ArrayUtils.addAll(vectors, vv);
  }

  /**
   * Transfer vectors to destination HyperVectorWrapper.
   * Both this and destination must be of same type and have same number of vectors.
   * @param destination destination HyperVectorWrapper.
   */
  @Override
  public void transfer(VectorWrapper<?> destination) {
    Preconditions.checkArgument(destination instanceof HyperVectorWrapper);
    Preconditions.checkArgument(getField().getType().equals(destination.getField().getType()));
    Preconditions.checkArgument(vectors.length == ((HyperVectorWrapper<?>)destination).vectors.length);

    ValueVector[] destionationVectors = ((HyperVectorWrapper<?>)destination).vectors;
    for (int i = 0; i < vectors.length; ++i) {
      vectors[i].makeTransferPair(destionationVectors[i]).transfer();
    }
  }
}
