package com.avaje.ebean.plugin;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;

import java.util.List;

/**
 * Extensions to EbeanServer API made available to plugins.
 */
public interface SpiServer extends EbeanServer {

  /**
   * Return the serverConfig.
   */
  ServerConfig getServerConfig();

  /**
   * Return the DatabasePlatform for this server.
   */
  DatabasePlatform getDatabasePlatform();

  /**
   * Return all the bean types registered on this server instance.
   */
  List<? extends BeanType<?>> getBeanTypes();

  /**
   * Return the bean type for a given entity bean class.
   */
  <T> BeanType<T> getBeanType(Class<T> beanClass);

  /**
   * Return the bean types mapped to the given base table.
   */
  List<? extends BeanType<?>> getBeanTypes(String baseTableName);

  /**
   * Return the bean type for a given doc store queueId.
   */
  BeanType<?> getBeanTypeForQueueId(String queueId);
}
