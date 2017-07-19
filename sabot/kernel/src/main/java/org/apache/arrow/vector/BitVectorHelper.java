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
package org.apache.arrow.vector;

import com.dremio.common.types.TypeProtos.MinorType;
import com.dremio.exec.proto.UserBitShared.SerializedField;
import com.google.common.base.Preconditions;
import io.netty.buffer.ArrowBuf;

public class BitVectorHelper extends BaseValueVectorHelper {

  private BitVector vector;

  public BitVectorHelper(BitVector vector) {
    super(vector);
    this.vector = vector;
  }

  public void load(SerializedField metadata, ArrowBuf buffer) {
    Preconditions.checkArgument(vector.name.equals(metadata.getNamePart().getName()), "The field %s doesn't match the provided metadata %s.", vector.name, metadata);
    final int valueCount = metadata.getValueCount();
    final int expectedLength = vector.getSizeFromCount(valueCount);
    final int actualLength = metadata.getBufferLength();
    assert expectedLength == actualLength: "expected and actual buffer sizes do not match";

    vector.clear();
    vector.data = buffer.slice(0, actualLength);
    vector.data.writerIndex(actualLength);
    vector.data.retain();
    vector.valueCount = valueCount;
  }

  @Override
  public SerializedField.Builder getMetadataBuilder() {
    SerializedField.Builder builder = super.getMetadataBuilder();
    return builder.setMajorType(com.dremio.common.types.Types.required(MinorType.BIT));
  }
}
