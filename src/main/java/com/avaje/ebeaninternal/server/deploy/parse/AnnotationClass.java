package com.avaje.ebeaninternal.server.deploy.parse;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.avaje.ebean.annotation.CacheStrategy;
import com.avaje.ebean.annotation.CacheTuning;
import com.avaje.ebean.annotation.DocStore;
import com.avaje.ebean.annotation.DbComment;
import com.avaje.ebean.annotation.Draftable;
import com.avaje.ebean.annotation.DraftableElement;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import com.avaje.ebean.annotation.History;
import com.avaje.ebean.annotation.Index;
import com.avaje.ebean.annotation.NamedUpdate;
import com.avaje.ebean.annotation.NamedUpdates;
import com.avaje.ebean.annotation.ReadAudit;
import com.avaje.ebean.annotation.UpdateMode;
import com.avaje.ebean.config.TableName;
import com.avaje.ebeaninternal.server.core.CacheOptions;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor.EntityType;
import com.avaje.ebeaninternal.server.deploy.CompoundUniqueConstraint;
import com.avaje.ebeaninternal.server.deploy.DeployNamedQuery;
import com.avaje.ebeaninternal.server.deploy.DeployNamedUpdate;
import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read the class level deployment annotations.
 */
public class AnnotationClass extends AnnotationParser {

  private static final Logger logger = LoggerFactory.getLogger(AnnotationClass.class);

  private final String asOfViewSuffix;

  private final String versionsBetweenSuffix;

  /**
   * Create for normal early parse of class level annotations.
   */
  public AnnotationClass(DeployBeanInfo<?> info, boolean validationAnnotations, String asOfViewSuffix, String versionsBetweenSuffix) {
    super(info, validationAnnotations);
    this.asOfViewSuffix = asOfViewSuffix;
    this.versionsBetweenSuffix = versionsBetweenSuffix;
  }

  /**
   * Create to parse AttributeOverride annotations which is run last
   * after all the properties/fields have been parsed fully.
   */
  public AnnotationClass(DeployBeanInfo<?> info) {
    super(info, false);
    this.asOfViewSuffix = null;
    this.versionsBetweenSuffix = null;
  }

  /**
   * Parse any AttributeOverride set on the class.
   */
  public void parseAttributeOverride() {

    Class<?> cls = descriptor.getBeanType();
    AttributeOverride override = cls.getAnnotation(AttributeOverride.class);
    if (override != null) {
      String propertyName = override.name();
      Column column = override.column();
      if (column != null) {
        DeployBeanProperty beanProperty = descriptor.getBeanProperty(propertyName);
        if (beanProperty == null) {
          logger.error("AttributeOverride property [" + propertyName + "] not found on " + descriptor.getFullName());
        } else {
          readColumn(column, beanProperty);
        }
      }
    }
  }

  /**
   * Read the class level deployment annotations.
   */
  public void parse() {
    read(descriptor.getBeanType());
    setTableName();
  }

  /**
   * Set the table name if it has not already been set.
   */
  private void setTableName() {

    if (descriptor.isBaseTableType()) {

      // default the TableName using NamingConvention.
      TableName tableName = namingConvention.getTableName(descriptor.getBeanType());

      descriptor.setBaseTable(tableName, asOfViewSuffix, versionsBetweenSuffix);
    }
  }

  private void read(Class<?> cls) {

    Entity entity = cls.getAnnotation(Entity.class);
    if (entity != null) {
      if (entity.name().equals("")) {
        descriptor.setName(cls.getSimpleName());
      } else {
        descriptor.setName(entity.name());
      }
    }

    Embeddable embeddable = cls.getAnnotation(Embeddable.class);
    if (embeddable != null) {
      descriptor.setEntityType(EntityType.EMBEDDED);
      descriptor.setName("Embeddable:" + cls.getSimpleName());
    }

    Index index = cls.getAnnotation(Index.class);
    if (index != null) {
      descriptor.addCompoundUniqueConstraint(new CompoundUniqueConstraint(index.columnNames(), index.name(), index.unique()));
    }

    UniqueConstraint uc = cls.getAnnotation(UniqueConstraint.class);
    if (uc != null) {
      descriptor.addCompoundUniqueConstraint(new CompoundUniqueConstraint(uc.columnNames()));
    }

    Table table = cls.getAnnotation(Table.class);
    if (table != null) {
      UniqueConstraint[] uniqueConstraints = table.uniqueConstraints();
      if (uniqueConstraints != null) {
        for (UniqueConstraint c : uniqueConstraints) {
          descriptor.addCompoundUniqueConstraint(new CompoundUniqueConstraint(c.columnNames()));
        }
      }
    }

    Draftable draftable = cls.getAnnotation(Draftable.class);
    if (draftable != null) {
      descriptor.setDraftable();
    }

    DraftableElement draftableElement = cls.getAnnotation(DraftableElement.class);
    if (draftableElement != null) {
      descriptor.setDraftableElement();
    }

    ReadAudit readAudit = cls.getAnnotation(ReadAudit.class);
    if (readAudit != null) {
      descriptor.setReadAuditing();
    }

    History history = cls.getAnnotation(History.class);
    if (history != null) {
      descriptor.setHistorySupport();
    }

    DbComment comment = cls.getAnnotation(DbComment.class);
    if (comment != null) {
      descriptor.setDbComment(comment.value());
    }

    DocStore elasticIndex = cls.getAnnotation(DocStore.class);
    if (elasticIndex != null) {
      descriptor.readElasticIndex(elasticIndex);
    }

    UpdateMode updateMode = cls.getAnnotation(UpdateMode.class);
    if (updateMode != null) {
      descriptor.setUpdateChangesOnly(updateMode.updateChangesOnly());
    }

    NamedQueries namedQueries = cls.getAnnotation(NamedQueries.class);
    if (namedQueries != null) {
      readNamedQueries(namedQueries);
    }
    NamedQuery namedQuery = cls.getAnnotation(NamedQuery.class);
    if (namedQuery != null) {
      readNamedQuery(namedQuery);
    }

    NamedUpdates namedUpdates = cls.getAnnotation(NamedUpdates.class);
    if (namedUpdates != null) {
      readNamedUpdates(namedUpdates);
    }

    NamedUpdate namedUpdate = cls.getAnnotation(NamedUpdate.class);
    if (namedUpdate != null) {
      readNamedUpdate(namedUpdate);
    }

    CacheStrategy cacheStrategy = cls.getAnnotation(CacheStrategy.class);
    CacheTuning cacheTuning = cls.getAnnotation(CacheTuning.class);
    if (cacheStrategy != null || cacheTuning != null) {
      readCacheStrategy(cacheStrategy, cacheTuning);
    }

    EntityConcurrencyMode entityConcurrencyMode = cls.getAnnotation(EntityConcurrencyMode.class);
    if (entityConcurrencyMode != null) {
      descriptor.setConcurrencyMode(entityConcurrencyMode.value());
    }
  }

  private void readCacheStrategy(CacheStrategy cacheStrategy, CacheTuning cacheTuning) {

    CacheOptions cacheOptions = descriptor.getCacheOptions();
    if (cacheTuning != null) {
      cacheOptions.setMaxSecsToLive(cacheTuning.maxSecsToLive());
      cacheOptions.setMaxIdleSecs(cacheTuning.maxIdleSecs()); 
    }
    if (cacheStrategy != null) {
      cacheOptions.setUseCache(cacheStrategy.useBeanCache());
      cacheOptions.setReadOnly(cacheStrategy.readOnly());
      cacheOptions.setWarmingQuery(cacheStrategy.warmingQuery());
      if (cacheStrategy.naturalKey().length() > 0) {
        String propName = cacheStrategy.naturalKey().trim();
        DeployBeanProperty beanProperty = descriptor.getBeanProperty(propName);
        if (beanProperty != null) {
          beanProperty.setNaturalKey();
          cacheOptions.setNaturalKey(propName);
        }
      }
    }
  }

  private void readNamedQueries(NamedQueries namedQueries) {
    NamedQuery[] queries = namedQueries.value();
    for (int i = 0; i < queries.length; i++) {
      readNamedQuery(queries[i]);
    }
  }

  private void readNamedQuery(NamedQuery namedQuery) {
    DeployNamedQuery q = new DeployNamedQuery(namedQuery);
    descriptor.add(q);
  }

  private void readNamedUpdates(NamedUpdates updates) {
    NamedUpdate[] updateArray = updates.value();
    for (int i = 0; i < updateArray.length; i++) {
      readNamedUpdate(updateArray[i]);
    }
  }

  private void readNamedUpdate(NamedUpdate update) {
    DeployNamedUpdate upd = new DeployNamedUpdate(update);
    descriptor.add(upd);
  }

}
