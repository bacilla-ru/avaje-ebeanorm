package com.avaje.ebeaninternal.server.type;

import com.avaje.ebean.config.dbplatform.DbType;
import com.avaje.ebeanservice.docstore.api.mapping.DocPropertyType;
import com.avaje.ebean.text.TextException;
import com.avaje.ebean.text.json.EJson;
import com.avaje.ebean.text.json.JsonWriter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Postgres Hstore type which maps Map<String,String> to a single 'HStore column' in the DB.
 */
@SuppressWarnings("rawtypes")
public class ScalarTypePostgresHstore extends ScalarTypeBase<Map> {

  public ScalarTypePostgresHstore() {
    super(Map.class, false, DbType.HSTORE);
  }

  @Override
  public boolean isMutable() {
    return true;
  }

  @Override
  public boolean isDirty(Object value) {
    return !(value instanceof ModifyAwareOwner) || ((ModifyAwareOwner) value).isMarkedDirty();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map read(DataReader dataReader) throws SQLException {

    Object value = dataReader.getObject();
    if (value == null) {
      return null;
    }
    if (!(value instanceof Map)) {
      throw new RuntimeException("Expecting Hstore to return as Map but got type " + value.getClass());
    }
    return new ModifyAwareMap((Map) value);
  }

  @Override
  public void bind(DataBind b, Map value) throws SQLException {
    b.setObject(value);
  }

  @Override
  public Object toJdbcType(Object value) {
    return value;
  }

  @Override
  public Map toBeanType(Object value) {
    return (Map) value;
  }

  @Override
  public String formatValue(Map v) {
    try {
      return EJson.write(v);
    } catch (IOException e) {
      throw new TextException(e);
    }
  }

  @Override
  public Map parse(String value) {
    try {
      return EJson.parseObject(value);
    } catch (IOException e) {
      throw new TextException(e);
    }
  }

  @Override
  public Map convertFromMillis(long dateTime) {
    throw new RuntimeException("Should never be called");
  }

  @Override
  public boolean isDateTimeCapable() {
    return false;
  }

  @Override
  public Map readData(DataInput dataInput) throws IOException {
    if (!dataInput.readBoolean()) {
      return null;
    } else {
      String json = dataInput.readUTF();
      return parse(json);
    }
  }

  @Override
  public void writeData(DataOutput dataOutput, Map v) throws IOException {
    if (v == null) {
      dataOutput.writeBoolean(false);
    } else {
      dataOutput.writeBoolean(true);
      String json = format(v);
      dataOutput.writeUTF(json);
    }
  }

  @Override
  public void jsonWrite(JsonWriter writer, String name, Map value) throws IOException {
    // write the field name followed by the Map/JSON Object
    if (value == null) {
      writer.writeNullField(name);
    } else {
      writer.writeFieldName(name);
      EJson.write(value, writer.gen());
    }
  }

  @Override
  public Map jsonRead(JsonParser parser, JsonToken event) throws IOException {
    // at this point the BeanProperty has read the START_OBJECT token
    // to check for a null value. Pass the START_OBJECT token through to
    // the EJson parsing so that it knows the first token has been read
    return EJson.parseObject(parser, event);
  }

  @Override
  public DocPropertyType getDocType() {
    return DocPropertyType.OBJECT;
  }
}
