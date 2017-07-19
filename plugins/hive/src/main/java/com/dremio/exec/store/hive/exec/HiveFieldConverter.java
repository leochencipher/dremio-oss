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
package com.dremio.exec.store.hive.exec;

import static com.dremio.exec.store.hive.HiveUtilities.throwUnsupportedHiveDataTypeError;

import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.arrow.vector.NullableBigIntVector;
import org.apache.arrow.vector.NullableBitVector;
import org.apache.arrow.vector.NullableDateMilliVector;
import org.apache.arrow.vector.NullableDecimalVector;
import org.apache.arrow.vector.NullableFloat4Vector;
import org.apache.arrow.vector.NullableFloat8Vector;
import org.apache.arrow.vector.NullableIntVector;
import org.apache.arrow.vector.NullableTimeStampMilliVector;
import org.apache.arrow.vector.NullableVarBinaryVector;
import org.apache.arrow.vector.NullableVarCharVector;
import org.apache.arrow.vector.ValueVector;
import org.apache.arrow.vector.holders.DecimalHolder;
import org.apache.arrow.vector.util.DecimalUtility;
import org.apache.hadoop.hive.serde2.io.DateWritable;
import org.apache.hadoop.hive.serde2.io.TimestampWritable;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.BinaryObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.BooleanObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.ByteObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.DateObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.DoubleObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.FloatObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveCharObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveDecimalObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveVarcharObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.IntObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.LongObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.ShortObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.StringObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.TimestampObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.DecimalTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.io.Text;
import com.dremio.sabot.exec.context.OperatorContext;
import com.google.common.collect.Maps;

public abstract class HiveFieldConverter {

  public abstract void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex);

  private static Map<PrimitiveCategory, Class< ? extends HiveFieldConverter>> primMap = Maps.newHashMap();

  // TODO (DRILL-2470)
  // Byte and short (tinyint and smallint in SQL types) are currently read as integers
  // as these smaller integer types are not fully supported in Dremio today.
  // Here the same types are used, as we have to read out of the correct typed converter
  // from the hive side, in the FieldConverter classes below for Byte and Short we convert
  // to integer when writing into Dremio's vectors.
  static {
    primMap.put(PrimitiveCategory.BINARY, Binary.class);
    primMap.put(PrimitiveCategory.BOOLEAN, Boolean.class);
    primMap.put(PrimitiveCategory.BYTE, Byte.class);
    primMap.put(PrimitiveCategory.DOUBLE, Double.class);
    primMap.put(PrimitiveCategory.FLOAT, Float.class);
    primMap.put(PrimitiveCategory.INT, Int.class);
    primMap.put(PrimitiveCategory.LONG, Long.class);
    primMap.put(PrimitiveCategory.SHORT, Short.class);
    primMap.put(PrimitiveCategory.STRING, String.class);
    primMap.put(PrimitiveCategory.VARCHAR, VarChar.class);
    primMap.put(PrimitiveCategory.TIMESTAMP, Timestamp.class);
    primMap.put(PrimitiveCategory.DATE, Date.class);
    primMap.put(PrimitiveCategory.CHAR, Char.class);
  }


  public static HiveFieldConverter create(TypeInfo typeInfo, OperatorContext context)
      throws IllegalAccessException, InstantiationException {
    switch (typeInfo.getCategory()) {
      case PRIMITIVE:
        final PrimitiveCategory pCat = ((PrimitiveTypeInfo) typeInfo).getPrimitiveCategory();
        if (pCat != PrimitiveCategory.DECIMAL) {
          Class<? extends HiveFieldConverter> clazz = primMap.get(pCat);
          if (clazz != null) {
            return clazz.newInstance();
          }
        } else {
          // For decimal, based on precision return appropriate converter.
          DecimalTypeInfo decimalTypeInfo = (DecimalTypeInfo) typeInfo;
          int precision = decimalTypeInfo.precision();
          int scale = decimalTypeInfo.scale();
          return new Decimal(precision, scale, context);
        }

        throwUnsupportedHiveDataTypeError(pCat.toString());
        break;

      case LIST:
      case MAP:
      case STRUCT:
      case UNION:
      default:
        throwUnsupportedHiveDataTypeError(typeInfo.getCategory().toString());
    }

    return null;
  }

  public static class Binary extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final byte[] value = ((BinaryObjectInspector)oi).getPrimitiveJavaObject(hiveFieldValue);
      ((NullableVarBinaryVector) outputVV).getMutator().setSafe(outputIndex, value, 0, value.length);
    }
  }

  public static class Boolean extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final boolean value = (boolean) ((BooleanObjectInspector)oi).getPrimitiveJavaObject(hiveFieldValue);
      ((NullableBitVector) outputVV).getMutator().setSafe(outputIndex, value ? 1 : 0);
    }
  }

  public static class Decimal extends HiveFieldConverter {
    private final DecimalHolder holder = new DecimalHolder();

    public Decimal(int precision, int scale, OperatorContext context) {
      holder.scale = scale;
      holder.precision = precision;
      holder.buffer = context.getManagedBuffer(16);
      holder.start = 0;
    }

    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      DecimalUtility.writeBigDecimalToArrowBuf(((HiveDecimalObjectInspector)oi).getPrimitiveJavaObject(hiveFieldValue).bigDecimalValue().setScale(holder.scale, RoundingMode.HALF_UP), holder.buffer, holder.start);
      ((NullableDecimalVector) outputVV).getMutator().setSafe(outputIndex, 1, 0, holder.buffer);
    }
  }

  public static class Double extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final double value = (double) ((DoubleObjectInspector)oi).getPrimitiveJavaObject(hiveFieldValue);
      ((NullableFloat8Vector) outputVV).getMutator().setSafe(outputIndex, value);
    }
  }

  public static class Float extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final float value = (float) ((FloatObjectInspector)oi).getPrimitiveJavaObject(hiveFieldValue);
      ((NullableFloat4Vector) outputVV).getMutator().setSafe(outputIndex, value);
    }
  }

  public static class Int extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final int value = (int) ((IntObjectInspector)oi).getPrimitiveJavaObject(hiveFieldValue);
      ((NullableIntVector) outputVV).getMutator().setSafe(outputIndex, value);
    }
  }

  // TODO (DRILL-2470)
  // Byte and short (tinyint and smallint in SQL types) are currently read as integers
  // as these smaller integer types are not fully supported in Dremio today.
  public static class Short extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final int value = (short) ((ShortObjectInspector)oi).getPrimitiveJavaObject(hiveFieldValue);
      ((NullableIntVector) outputVV).getMutator().setSafe(outputIndex, value);
    }
  }

  public static class Byte extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final int value = (byte)((ByteObjectInspector)oi).getPrimitiveJavaObject(hiveFieldValue);
      ((NullableIntVector) outputVV).getMutator().setSafe(outputIndex, value);
    }
  }

  public static class Long extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final long value = (long) ((LongObjectInspector)oi).getPrimitiveJavaObject(hiveFieldValue);
      ((NullableBigIntVector) outputVV).getMutator().setSafe(outputIndex, value);
    }
  }

  public static class String extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final Text value = ((StringObjectInspector)oi).getPrimitiveWritableObject(hiveFieldValue);
      final byte[] valueBytes = value.getBytes();
      final int len = value.getLength();
      ((NullableVarCharVector) outputVV).getMutator().setSafe(outputIndex, valueBytes, 0, len);
    }
  }

  public static class VarChar extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final Text value = ((HiveVarcharObjectInspector)oi).getPrimitiveWritableObject(hiveFieldValue).getTextValue();
      final byte[] valueBytes = value.getBytes();
      final int valueLen = value.getLength();
      ((NullableVarCharVector) outputVV).getMutator().setSafe(outputIndex, valueBytes, 0, valueLen);
    }
  }

  public static class Timestamp extends HiveFieldConverter {

    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final TimestampWritable value = ((TimestampObjectInspector)oi).getPrimitiveWritableObject(hiveFieldValue);
      long seconds = value.getSeconds();
      long nanos = value.getNanos();
      long millis = seconds * 1000 + nanos/1000/1000;
      ((NullableTimeStampMilliVector) outputVV).getMutator().setSafe(outputIndex, millis);
    }
  }

  public static class Date extends HiveFieldConverter {

    private static final long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1L);

    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final DateWritable writeable = ((DateObjectInspector)oi).getPrimitiveWritableObject(hiveFieldValue);
      ((NullableDateMilliVector) outputVV).getMutator().setSafe(outputIndex, writeable.getDays() * MILLIS_PER_DAY);
    }
  }

  public static class Char extends HiveFieldConverter {
    @Override
    public void setSafeValue(ObjectInspector oi, Object hiveFieldValue, ValueVector outputVV, int outputIndex) {
      final Text value = ((HiveCharObjectInspector)oi).getPrimitiveWritableObject(hiveFieldValue).getStrippedValue();
      final byte[] valueBytes = value.getBytes();
      final int valueLen = value.getLength();
      ((NullableVarCharVector) outputVV).getMutator().setSafe(outputIndex, valueBytes, 0, valueLen);
    }
  }

}
