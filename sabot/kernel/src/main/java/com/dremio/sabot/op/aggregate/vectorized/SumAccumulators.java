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
package com.dremio.sabot.op.aggregate.vectorized;

import java.util.List;

import org.apache.arrow.vector.FieldVector;

import com.dremio.sabot.op.common.ht2.LBlockHashTable;

import io.netty.buffer.ArrowBuf;
import io.netty.util.internal.PlatformDependent;

public class SumAccumulators {

  private SumAccumulators(){};

  public static class IntSumAccumulator extends BaseSingleAccumulator {

    private static final int WIDTH = 4;

    public IntSumAccumulator(FieldVector input, FieldVector output) {
      super(input, output);
    }

    @Override
    void initialize(FieldVector vector) {
      setNullAndZero(vector);
    }

    public void accumulate(final long memoryAddr, final int count) {
      final long maxAddr = memoryAddr + count * 4;
      List<ArrowBuf> buffers = getInput().getFieldBuffers();
      final long incomingBit = buffers.get(0).memoryAddress();
      final long incomingValue = buffers.get(1).memoryAddress();
      final long[] bitAddresses = this.bitAddresses;
      final long[] valueAddresses = this.valueAddresses;

      int incomingIndex = 0;
      for(long ordinalAddr = memoryAddr; ordinalAddr < maxAddr; ordinalAddr += 4, incomingIndex++){
        final int bitVal = (PlatformDependent.getByte(incomingBit + ((incomingIndex >>> 3))) >>> (incomingIndex & 7)) & 1;
        final int newVal = PlatformDependent.getInt(incomingValue + (incomingIndex * WIDTH)) * bitVal;
        final int tableIndex = PlatformDependent.getInt(ordinalAddr);
        int chunkIndex = tableIndex >>> LBlockHashTable.BITS_IN_CHUNK;
        int chunkOffset = tableIndex & LBlockHashTable.CHUNK_OFFSET_MASK;
        final long sumAddr = valueAddresses[chunkIndex] + (chunkOffset) * 8;
        final long bitUpdateAddr = bitAddresses[chunkIndex] + ((chunkOffset >>> 5) * 4);
        final int bitUpdateVal = bitVal << (chunkOffset & 31);
        PlatformDependent.putLong(sumAddr, PlatformDependent.getLong(sumAddr) + newVal);
        PlatformDependent.putInt(bitUpdateAddr, PlatformDependent.getInt(bitUpdateAddr) | bitUpdateVal);
      }
    }
  }

  public static class FloatSumAccumulator extends BaseSingleAccumulator {

    private static final int WIDTH = 4;

    public FloatSumAccumulator(FieldVector input, FieldVector output) {
      super(input, output);
    }

    @Override
    void initialize(FieldVector vector) {
      setNullAndZero(vector);
    }

    public void accumulate(final long memoryAddr, final int count) {
      final long maxAddr = memoryAddr + count * 4;
      List<ArrowBuf> buffers = getInput().getFieldBuffers();
      final long incomingBit = buffers.get(0).memoryAddress();
      final long incomingValue = buffers.get(1).memoryAddress();
      final long[] bitAddresses = this.bitAddresses;
      final long[] valueAddresses = this.valueAddresses;

      int incomingIndex = 0;
      for(long ordinalAddr = memoryAddr; ordinalAddr < maxAddr; ordinalAddr += 4, incomingIndex++){
        final int bitVal = (PlatformDependent.getByte(incomingBit + ((incomingIndex >>> 3))) >>> (incomingIndex & 7)) & 1;
        final float newVal = Float.intBitsToFloat(PlatformDependent.getInt(incomingValue + (incomingIndex * WIDTH)) * bitVal);
        final int tableIndex = PlatformDependent.getInt(ordinalAddr);
        int chunkIndex = tableIndex >>> LBlockHashTable.BITS_IN_CHUNK;
        int chunkOffset = tableIndex & LBlockHashTable.CHUNK_OFFSET_MASK;
        final long sumAddr = valueAddresses[chunkIndex] + (chunkOffset) * 8;
        final long bitUpdateAddr = bitAddresses[chunkIndex] + ((chunkOffset >>> 5) * 4);
        final int bitUpdateVal = bitVal << (chunkOffset & 31);
        PlatformDependent.putLong(sumAddr, Double.doubleToLongBits(Double.longBitsToDouble(PlatformDependent.getLong(sumAddr)) + newVal));
        PlatformDependent.putInt(bitUpdateAddr, PlatformDependent.getInt(bitUpdateAddr) | bitUpdateVal);
      }
    }
  }

  public static class BigIntSumAccumulator extends BaseSingleAccumulator {

    private static final int WIDTH = 8;

    public BigIntSumAccumulator(FieldVector input, FieldVector output) {
      super(input, output);
    }

    @Override
    void initialize(FieldVector vector) {
      setNullAndZero(vector);
    }

    public void accumulate(final long memoryAddr, final int count) {
      final long maxAddr = memoryAddr + count * 4;
      List<ArrowBuf> buffers = getInput().getFieldBuffers();
      final long incomingBit = buffers.get(0).memoryAddress();
      final long incomingValue = buffers.get(1).memoryAddress();
      final long[] bitAddresses = this.bitAddresses;
      final long[] valueAddresses = this.valueAddresses;

      int incomingIndex = 0;
      for(long ordinalAddr = memoryAddr; ordinalAddr < maxAddr; ordinalAddr += 4, incomingIndex++){
        final int bitVal = (PlatformDependent.getByte(incomingBit + ((incomingIndex >>> 3))) >>> (incomingIndex & 7)) & 1;
        final long newVal = PlatformDependent.getLong(incomingValue + (incomingIndex * WIDTH)) * bitVal;
        final int tableIndex = PlatformDependent.getInt(ordinalAddr);
        int chunkIndex = tableIndex >>> LBlockHashTable.BITS_IN_CHUNK;
        int chunkOffset = tableIndex & LBlockHashTable.CHUNK_OFFSET_MASK;
        final long sumAddr = valueAddresses[chunkIndex] + (chunkOffset) * 8;
        final long bitUpdateAddr = bitAddresses[chunkIndex] + ((chunkOffset >>> 5) * 4);
        final int bitUpdateVal = bitVal << (chunkOffset & 31);
        PlatformDependent.putLong(sumAddr, PlatformDependent.getLong(sumAddr) + newVal);
        PlatformDependent.putInt(bitUpdateAddr, PlatformDependent.getInt(bitUpdateAddr) | bitUpdateVal);
      }
    }
  }


  public static class DoubleSumAccumulator extends BaseSingleAccumulator {

    private static final int WIDTH = 8;

    public DoubleSumAccumulator(FieldVector input, FieldVector output) {
      super(input, output);
    }

    @Override
    void initialize(FieldVector vector) {
      setNullAndZero(vector);
    }

    public void accumulate(final long memoryAddr, final int count) {
      final long maxAddr = memoryAddr + count * 4;
      List<ArrowBuf> buffers = getInput().getFieldBuffers();
      final long incomingBit = buffers.get(0).memoryAddress();
      final long incomingValue = buffers.get(1).memoryAddress();
      final long[] bitAddresses = this.bitAddresses;
      final long[] valueAddresses = this.valueAddresses;

      int incomingIndex = 0;
      for(long ordinalAddr = memoryAddr; ordinalAddr < maxAddr; ordinalAddr += 4, incomingIndex++){
        final int bitVal = (PlatformDependent.getByte(incomingBit + ((incomingIndex >>> 3))) >>> (incomingIndex & 7)) & 1;
        final double newVal = Double.longBitsToDouble(PlatformDependent.getLong(incomingValue + (incomingIndex * WIDTH)) * bitVal);
        final int tableIndex = PlatformDependent.getInt(ordinalAddr);
        int chunkIndex = tableIndex >>> LBlockHashTable.BITS_IN_CHUNK;
        int chunkOffset = tableIndex & LBlockHashTable.CHUNK_OFFSET_MASK;
        final long sumAddr = valueAddresses[chunkIndex] + (chunkOffset) * 8;
        final long bitUpdateAddr = bitAddresses[chunkIndex] + ((chunkOffset >>> 5) * 4);
        final int bitUpdateVal = bitVal << (chunkOffset & 31);
        PlatformDependent.putLong(sumAddr, Double.doubleToLongBits(Double.longBitsToDouble(PlatformDependent.getLong(sumAddr)) + newVal));
        PlatformDependent.putInt(bitUpdateAddr, PlatformDependent.getInt(bitUpdateAddr) | bitUpdateVal);
      }
    }
  }

}
