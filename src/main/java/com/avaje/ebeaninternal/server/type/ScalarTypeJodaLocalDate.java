package com.avaje.ebeaninternal.server.type;

import java.sql.Date;
import java.sql.Types;

import org.joda.time.LocalDate;

import com.avaje.ebeaninternal.server.core.BasicTypeConverter;

/**
 * ScalarType for Joda LocalDate. This maps to a JDBC Date.
 */
public class ScalarTypeJodaLocalDate extends ScalarTypeBaseDate<LocalDate> {

  public ScalarTypeJodaLocalDate() {
    super(LocalDate.class, false, Types.DATE);
  }

  @Override
  public long convertToMillis(LocalDate value) {
    return value.toDateTimeAtStartOfDay().getMillis();
  }

  @Override
  public LocalDate convertFromDate(Date ts) {
    return new LocalDate(ts.getTime());
  }

  @Override
  public Date convertToDate(LocalDate t) {
    return new java.sql.Date(t.toDateTimeAtStartOfDay().getMillis());
  }

  @Override
  public Object toJdbcType(Object value) {
    if (value instanceof LocalDate) {
      return new java.sql.Date(((LocalDate) value).toDateTimeAtStartOfDay().getMillis());
    }
    return BasicTypeConverter.toDate(value);
  }

  @Override
  public LocalDate toBeanType(Object value) {
    if (value instanceof java.util.Date) {
      return new LocalDate(((java.util.Date) value).getTime());
    }
    return (LocalDate) value;
  }

  @Override
  public LocalDate convertFromMillis(long systemTimeMillis) {
    return new LocalDate(systemTimeMillis);
  }

}
