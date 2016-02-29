package com.avaje.ebeanservice.docstore.none;

import com.avaje.ebeaninternal.server.core.PersistRequestBean;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebeanservice.docstore.api.DocStoreUpdateContext;
import com.avaje.ebeanservice.docstore.api.support.DocStoreBeanBaseAdapter;

import java.io.IOException;

/**
 * DocStoreBeanBaseAdapter that barfs if it is used.
 */
public class NoneDocStoreBeanAdapter<T> extends DocStoreBeanBaseAdapter<T> {

  public NoneDocStoreBeanAdapter(BeanDescriptor<T> desc, DeployBeanDescriptor<T> deploy) {
    super(desc, deploy);
  }

  @Override
  public void deleteById(Object idValue, DocStoreUpdateContext txn) throws IOException {
    throw NoneDocStore.implementationNotInClassPath();
  }

  @Override
  public void index(Object idValue, T entityBean, DocStoreUpdateContext txn) throws IOException {
    throw NoneDocStore.implementationNotInClassPath();
  }

  @Override
  public void insert(Object idValue, PersistRequestBean<T> persistRequest, DocStoreUpdateContext txn) throws IOException {
    throw NoneDocStore.implementationNotInClassPath();
  }

  @Override
  public void update(Object idValue, PersistRequestBean<T> persistRequest, DocStoreUpdateContext txn) throws IOException {
    throw NoneDocStore.implementationNotInClassPath();
  }

  @Override
  public void updateEmbedded(Object idValue, String embeddedProperty, String embeddedRawContent, DocStoreUpdateContext txn) throws IOException {
    throw NoneDocStore.implementationNotInClassPath();
  }
}
