package com.avaje.ebeanservice.docstore.api.mapping;

import com.avaje.ebean.text.PathProperties;

/**
 * Mapping for a document stored in a doc store (like ElasticSearch).
 */
public class DocumentMapping {

  protected final String queueId;

  protected final String name;

  protected final String type;

  protected final PathProperties paths;

  protected final DocPropertyMapping properties;

  protected int shards;

  protected int replicas;

  public DocumentMapping(String queueId, String name, String type, PathProperties paths, DocPropertyMapping properties, int shards, int replicas) {
    this.queueId = queueId;
    this.name = name;
    this.type = type;
    this.paths = paths;
    this.properties = properties;
    this.shards = shards;
    this.replicas = replicas;
  }

  /**
   * Visit all the properties in the document structure.
   */
  public void visit(DocPropertyVisitor visitor) {
    properties.visit(visitor);
  }

  /**
   * Return the queueId.
   */
  public String getQueueId() {
    return queueId;
  }

  /**
   * Return the name.
   */
  public String getName() {
    return name;
  }

  /**
   * Return the type.
   */
  public String getType() {
    return type;
  }

  /**
   * Return the document structure as PathProperties.
   */
  public PathProperties getPaths() {
    return paths;
  }

  /**
   * Return the document structure with mapping details.
   */
  public DocPropertyMapping getProperties() {
    return properties;
  }

  /**
   * Return the number of shards.
   */
  public int getShards() {
    return shards;
  }

  /**
   * Set the number of shards.
   */
  public void setShards(int shards) {
    this.shards = shards;
  }

  /**
   * Return the number of replicas.
   */
  public int getReplicas() {
    return replicas;
  }

  /**
   * Set the number of replicas.
   */
  public void setReplicas(int replicas) {
    this.replicas = replicas;
  }
}