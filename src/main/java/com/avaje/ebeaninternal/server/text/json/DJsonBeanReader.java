package com.avaje.ebeaninternal.server.text.json;

import com.avaje.ebean.PersistenceIOException;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.text.json.JsonBeanReader;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

/**
 * A 'context' for reading entity beans from JSON.
 * <p>
 *   This is used such that a load context and persistence context can be used to span multiple marshalling requests.
 * </p>
 */
public class DJsonBeanReader<T> implements JsonBeanReader<T> {

  private final BeanDescriptor<T> desc;

  private final ReadJson readJson;

  public DJsonBeanReader(BeanDescriptor<T> desc, ReadJson readJson) {
    this.desc = desc;
    this.readJson = readJson;
  }

  @Override
  public void persistenceContextPut(Object beanId, T currentBean) {
    readJson.persistenceContextPut(beanId, currentBean);
  }

  @Override
  public PersistenceContext getPersistenceContext() {
    return readJson.getPersistenceContext();
  }

  @Override
  public T read() {
    try {
      return desc.jsonRead(readJson, null);
    } catch (IOException e) {
      throw new PersistenceIOException(e);
    }
  }

  @Override
  public JsonBeanReader<T> forJson(JsonParser moreJson) {
    return new DJsonBeanReader(desc, readJson.forJson(moreJson));
  }
}
