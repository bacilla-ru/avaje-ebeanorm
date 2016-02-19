package com.avaje.ebeanservice.docstore.api.support;

import com.avaje.ebean.DocStoreQueueEntry;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.plugin.SpiBeanType;
import com.avaje.ebeanservice.docstore.api.DocStoreUpdates;
import com.avaje.tests.model.basic.Order;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DocStoreIndexEventTest {

  static EbeanServer server = Ebean.getDefaultServer();

  <T> SpiBeanType<T> beanType(Class<T> cls) {
    return server.getPluginApi().getBeanType(cls);
  }

  SpiBeanType<Order> orderType() {
    return beanType(Order.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void docStoreUpdate() throws Exception {

    SpiBeanType<Order> mock = (SpiBeanType<Order>)Mockito.mock(SpiBeanType.class);

    Order bean = new Order();
    DocStoreIndexEvent<Order> event = new DocStoreIndexEvent<Order>(mock, 42, bean);

    event.docStoreUpdate(null);

    verify(mock, times(1)).docStoreIndex(42, bean, null);
  }

  @Test
  public void addToQueue() throws Exception {

    Order bean = new Order();

    DocStoreIndexEvent<Order> event = new DocStoreIndexEvent<Order>(orderType(), 42, bean);

    DocStoreUpdates updates = new DocStoreUpdates();
    event.addToQueue(updates);

    List<DocStoreQueueEntry> queueEntries = updates.getQueueEntries();
    assertThat(queueEntries).hasSize(1);

    DocStoreQueueEntry entry = queueEntries.get(0);
    assertThat(entry.getBeanId()).isEqualTo(42);
    assertThat(entry.getQueueId()).isEqualTo("order");
    assertThat(entry.getPath()).isNull();
    assertThat(entry.getType()).isEqualTo(DocStoreQueueEntry.Action.INDEX);
  }
}