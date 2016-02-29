package com.avaje.ebean;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Document storage operations.
 */
public interface DocumentStore {

  /**
   * Update the associated document store using the result of the query.
   * <p>
   * Note that the select and fetch paths of the query is set for you to match the
   * document structure needed for the index so what this query requires is the
   * predicates only.
   * </p>
   * <p>
   * This query will be executed using findEach so it is safe to use a query
   * that will fetch a lot of beans. The default bulkBatchSize is used.
   * </p>
   *
   * @param query The query used to update the associated document store.
   */
  <T> void indexByQuery(Query<T> query);

  /**
   * Update the associated ElasticSearch index using the result of the query additionally specifying a
   * bulkBatchSize to use for sending the messages to ElasticSearch.
   */
  <T> void indexByQuery(Query<T> query, int bulkBatchSize);

  /**
   * Index the index for all beans of this type.
   * <p>
   * This is the same as indexByQuery where the query has no predicates and so
   * fetches all rows.
   */
  void indexAll(Class<?> beanType);


  /**
   * Return the bean by fetching it's content from the document store.
   * If the document is not found null is returned.
   */
  @Nullable
  <T> T getById(Class<T> beanType, Object id);

  /**
   * Execute the query against the document store returning the list.
   */
  <T> List<T> findList(Query<T> query);

  /**
   * Execute the query against the document store with the expectation of a large set of results
   * that are processed in a scrolling resultSet fashion.
   * <p>
   * For example, with the ElasticSearch doc store this uses SCROLL.
   * </p>
   */
  <T> void findEach(Query<T> query, QueryEachConsumer<T> consumer);

  /**
   * Process the queue entries.
   */
  long process(List<DocStoreQueueEntry> queueEntries) throws IOException;

  /**
   * Drop the index.
   */
  void dropIndex(String newIndex);

  /**
   * Create an index given a mapping file as a resource in the classPath.
   *
   * @param indexName       the name of the new index
   * @param alias           the alias of the index
   * @param mappingResource the path of the mapping file as a resource in the classpath
   */
  void createIndex(String indexName, String alias, String mappingResource);

  /**
   * Copy the index to a new index.
   *
   * @param beanType The bean type of the source index
   * @param newIndex The name of the index to copy to
   *
   * @return the number of documents copied to the new index
   */
  long copyIndex(Class<?> beanType, String newIndex);

  /**
   * Copy entries from an index to a new index but limiting to documents that have been
   * modified since the sinceEpochMillis time.
   * <p>
   * To support this the document needs to have a <code>@WhenModified</code> property.
   * </p>
   *
   * @param beanType The bean type of the source index
   * @param newIndex The name of the index to copy to
   *
   * @return the number of documents copied to the new index
   */
  long copyIndex(Class<?> beanType, String newIndex, long sinceEpochMillis);

}
