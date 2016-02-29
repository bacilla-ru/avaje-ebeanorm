package com.avaje.ebeaninternal.server.type;

import com.avaje.ebeanservice.docstore.api.mapping.DocPropertyType;
import com.avaje.ebean.text.json.JsonWriter;
import com.avaje.ebeaninternal.server.core.BasicTypeConverter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Types;

/**
 * ScalarType for java.math.BigInteger.
 */
public class ScalarTypeMathBigInteger extends ScalarTypeBase<BigInteger> {

  public ScalarTypeMathBigInteger() {
    super(BigInteger.class, false, Types.BIGINT);
  }

  @Override
  public void bind(DataBind b, BigInteger value) throws SQLException {
    if (value == null) {
      b.setNull(Types.BIGINT);
    } else {
      b.setLong(value.longValue());
    }
  }

  @Override
  public BigInteger read(DataReader dataReader) throws SQLException {

    Long l = dataReader.getLong();
    if (l == null) {
      return null;
    }
    return new BigInteger(String.valueOf(l));
  }

  @Override
  public Object toJdbcType(Object value) {
    return BasicTypeConverter.toLong(value);
  }

  @Override
  public BigInteger toBeanType(Object value) {
    return BasicTypeConverter.toMathBigInteger(value);
  }

  @Override
  public String formatValue(BigInteger v) {
    return v.toString();
  }

  @Override
  public BigInteger parse(String value) {
    return new BigInteger(value);
  }

  @Override
  public BigInteger convertFromMillis(long systemTimeMillis) {
    return BigInteger.valueOf(systemTimeMillis);
  }

  @Override
  public boolean isDateTimeCapable() {
    return true;
  }

  @Override
  public BigInteger readData(DataInput dataInput) throws IOException {
    if (!dataInput.readBoolean()) {
      return null;
    } else {
      long val = dataInput.readLong();
      return BigInteger.valueOf(val);
    }
  }

  @Override
  public void writeData(DataOutput dataOutput, BigInteger value) throws IOException {

    if (value == null) {
      dataOutput.writeBoolean(false);
    } else {
      dataOutput.writeBoolean(true);
      dataOutput.writeLong(value.longValue());
    }
  }

  @Override
  public BigInteger jsonRead(JsonParser parser, JsonToken event) throws IOException {
    return parser.getDecimalValue().toBigInteger();
  }

  @Override
  public void jsonWrite(JsonWriter writer, String name, BigInteger value) throws IOException {
    writer.writeNumberField(name, value.longValue());
  }

  @Override
  public DocPropertyType getDocType() {
    return DocPropertyType.LONG;
  }

}
