package com.avaje.ebeaninternal.server.type;

import com.avaje.ebean.config.dbplatform.DbType;
import com.avaje.ebeanservice.docstore.api.mapping.DocPropertyType;
import com.avaje.ebean.text.json.JsonWriter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Postgres Hstore type which maps Map<String,String> to a single 'HStore column' in the DB.
 */
@SuppressWarnings("rawtypes")
public class ScalarTypeUUIDNative extends ScalarTypeBase<UUID> {

  public ScalarTypeUUIDNative() {
    super(UUID.class, false, DbType.UUID);
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public boolean isDirty(Object value) {
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public UUID read(DataReader dataReader) throws SQLException {

    Object value = dataReader.getObject();
    if (value == null) {
      return null;
    }
    return (UUID)value;
  }

  @Override
  public void bind(DataBind b, UUID value) throws SQLException {
    b.setObject(value);
  }

  @Override
  public Object toJdbcType(Object value) {
    return value;
  }

  @Override
  public UUID toBeanType(Object value) {
    return (UUID) value;
  }

  @Override
  public String formatValue(UUID v) {
    return v.toString();
  }

  @Override
  public UUID parse(String value) {
    return UUID.fromString(value);
  }

  @Override
  public UUID convertFromMillis(long dateTime) {
    throw new RuntimeException("Should never be called");
  }

  @Override
  public boolean isDateTimeCapable() {
    return false;
  }

  @Override
  public UUID readData(DataInput dataInput) throws IOException {
    if (!dataInput.readBoolean()) {
      return null;
    } else {
      String json = dataInput.readUTF();
      return parse(json);
    }
  }

  @Override
  public void writeData(DataOutput dataOutput, UUID v) throws IOException {
    if (v == null) {
      dataOutput.writeBoolean(false);
    } else {
      dataOutput.writeBoolean(true);
      String json = format(v);
      dataOutput.writeUTF(json);
    }
  }

  @Override
  public void jsonWrite(JsonWriter writer, String name, UUID value) throws IOException {
    // write the field name followed by the Map/JSON Object
    if (value == null) {
      writer.writeNullField(name);
    } else {
      writer.writeStringField(name, formatValue(value));
    }
  }

  @Override
  public UUID jsonRead(JsonParser parser, JsonToken event) throws IOException {
    // at this point the BeanProperty has read the START_OBJECT token
    // to check for a null value. Pass the START_OBJECT token through to
    // the EJson parsing so that it knows the first token has been read
    String strValue = parser.getValueAsString();
    return strValue == null ? null : parse(strValue);
  }

  @Override
  public DocPropertyType getDocType() {
    return DocPropertyType.STRING;
  }
}
