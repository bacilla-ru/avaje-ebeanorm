package com.avaje.ebeaninternal.server.deploy;

import java.util.ArrayList;

import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebeaninternal.server.core.InternString;
import com.avaje.ebeaninternal.server.deploy.id.IdBinder;
import com.avaje.ebeaninternal.server.deploy.id.ImportedId;
import com.avaje.ebeaninternal.server.deploy.id.ImportedIdEmbedded;
import com.avaje.ebeaninternal.server.deploy.id.ImportedIdSimple;
import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanPropertyAssoc;
import com.avaje.ebeaninternal.server.el.ElPropertyChainBuilder;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.query.SqlJoinType;

/**
 * Abstract base for properties mapped to an associated bean, list, set or map.
 */
public abstract class BeanPropertyAssoc<T> extends BeanProperty {

	private static final Logger logger = LoggerFactory.getLogger(BeanPropertyAssoc.class);

	/**
	 * The descriptor of the target. This MUST be initialised after construction
	 * so as to avoid a dependency loop between BeanDescriptors.
	 */
	BeanDescriptor<T> targetDescriptor;

	IdBinder targetIdBinder;

	InheritInfo targetInheritInfo;

	String targetIdProperty;

	/**
	 * Persist settings.
	 */
	final BeanCascadeInfo cascadeInfo;

	/**
	 * Join between the beans.
	 */
	final TableJoin tableJoin;

	/**
	 * The type of the joined bean.
	 */
	final Class<T> targetType;

	/**
	 * The join table information.
	 */
	final BeanTable beanTable;
	
	final String mappedBy;

  final String elasticDoc;
  
  final boolean elasticFlatten;

	final String extraWhere;

	boolean saveRecurseSkippable;

	/**
	 * Construct the property.
	 */
	public BeanPropertyAssoc(BeanDescriptor<?> descriptor, DeployBeanPropertyAssoc<T> deploy) {
		super(descriptor, deploy);
		this.extraWhere = InternString.intern(deploy.getExtraWhere());
		this.beanTable = deploy.getBeanTable();
		this.mappedBy = InternString.intern(deploy.getMappedBy());
    this.elasticDoc = deploy.getElasticDoc();
    this.elasticFlatten = deploy.isElasticFlatten();

		this.tableJoin = new TableJoin(deploy.getTableJoin());

		this.targetType = deploy.getTargetType();
		this.cascadeInfo = deploy.getCascadeInfo();
	}
	
	/**
	 * Initialise post construction.
	 */
	@Override
	public void initialise() {
		// this *MUST* execute after the BeanDescriptor is
		// put into the map to stop infinite recursion
    targetDescriptor = descriptor.getBeanDescriptor(targetType);
    if (!isTransient){
			targetIdBinder = targetDescriptor.getIdBinder();
			targetInheritInfo = targetDescriptor.getInheritInfo();
			saveRecurseSkippable = targetDescriptor.isSaveRecurseSkippable();

			if (!targetIdBinder.isComplexId()){
				targetIdProperty = targetIdBinder.getIdProperty();
			}
		}
	}
	
	/**
     * Create a ElPropertyValue for a *ToOne or *ToMany.
     */
    protected ElPropertyValue createElPropertyValue(String propName, String remainder, ElPropertyChainBuilder chain, boolean propertyDeploy) {
        
        // associated or embedded bean
        BeanDescriptor<?> embDesc = getTargetDescriptor();
    
        if (chain == null) {
            chain = new ElPropertyChainBuilder(isEmbedded(), propName);
        }
        chain.add(this);
        if (containsMany()) {
            chain.setContainsMany();
        }
        return embDesc.buildElGetValue(remainder, chain, propertyDeploy);
    }
	
    /**
	 * Add table join with table alias based on prefix.
	 */
    public SqlJoinType addJoin(SqlJoinType joinType, String prefix, DbSqlContext ctx) {
    	return tableJoin.addJoin(joinType, prefix, ctx);
    }
    
    /**
	 * Add table join with explicit table alias.
	 */
    public SqlJoinType addJoin(SqlJoinType joinType, String a1, String a2, DbSqlContext ctx) {
    	return tableJoin.addJoin(joinType, a1, a2, ctx);
    }
    
	/**
	 * Return false.
	 */
	public boolean isScalar() {
		return false;
	}
	
	/**
	 * Return the mappedBy property.
	 * This will be null on the owning side.
	 */
	public String getMappedBy() {
		return mappedBy;
	}

	/**
	 * Return the Id property of the target entity type.
	 * <p>
	 * This will return null for multiple Id properties.
	 * </p>
	 */
	public String getTargetIdProperty() {
		return targetIdProperty;
	}

	/**
	 * Return the BeanDescriptor of the target.
	 */
	public BeanDescriptor<T> getTargetDescriptor() {
		return targetDescriptor;
	}
	
	public boolean isSaveRecurseSkippable(Object bean) {

    return saveRecurseSkippable && bean instanceof EntityBean && !((EntityBean) bean)._ebean_getIntercept().isNewOrDirty();
  }

	/**
	 * Return true if save can be skipped for unmodified bean(s) of this
	 * property.
	 * <p>
	 * That is, if a bean of this property is unmodified we don't need to
	 * saveRecurse because none of its associated beans have cascade save set to
	 * true.
	 * </p>
	 */
	public boolean isSaveRecurseSkippable() {
		return saveRecurseSkippable;
	}

	/**
	 * Return true if the unique id properties are all not null for this bean.
	 */
	public boolean hasId(EntityBean bean) {

		BeanDescriptor<?> targetDesc = getTargetDescriptor();
		BeanProperty idProp = targetDesc.getIdProperty();
		if (idProp != null) {
			Object value = idProp.getValue(bean);
			if (value == null) {
				return false;
			}
		}
		// all the unique properties are non-null
		return true;
	}

	/**
	 * Return the type of the target.
	 * <p>
	 * This is the class of the associated bean, or beans contained in a list,
	 * set or map.
	 * </p>
	 */
	public Class<?> getTargetType() {
		return targetType;
	}

	/**
	 * Return an extra clause to add to the query for loading or joining
	 * to this bean type.
	 */
	public String getExtraWhere() {
		return extraWhere;
	}

  /**
   * Return the elastic search doc for this embedded property.
   */
  public String getElasticDoc() {
    return elasticDoc;
  }

  /**
   * Return if this elastic search property should be 'flattened'.
   */
  public boolean isElasticFlatten() {
    return elasticFlatten;
  }

	/**
	 * Return true if this association is updateable.
	 */
	public boolean isUpdateable() {
    return tableJoin.columns().length <= 0 || tableJoin.columns()[0].isUpdateable();
  }

	/**
	 * Return true if this association is insertable.
	 */
	public boolean isInsertable() {
    return tableJoin.columns().length <= 0 || tableJoin.columns()[0].isInsertable();
  }

	/**
	 * return the join to use for the bean.
	 */
	public TableJoin getTableJoin() {
		return tableJoin;
	}

	/**
	 * Get the persist info.
	 */
	public BeanCascadeInfo getCascadeInfo() {
		return cascadeInfo;
	}

	/**
	 * Build the list of imported property. Matches BeanProperty from the target
	 * descriptor back to local database columns in the TableJoin.
	 */
	protected ImportedId createImportedId(BeanPropertyAssoc<?> owner, BeanDescriptor<?> target, TableJoin join) {

		BeanProperty idProp = target.getIdProperty();
		BeanProperty[] others = target.propertiesBaseScalar();

		if (descriptor.isSqlSelectBased()){
			String dbColumn = owner.getDbColumn();
			return new ImportedIdSimple(owner, dbColumn, idProp, 0);
		}

		TableJoinColumn[] cols = join.columns();

		if (idProp == null) {
		  return null;
		}
		if (!idProp.isEmbedded()) {
			// simple single scalar id
			if (cols.length != 1){
				String msg = "No Imported Id column for ["+idProp+"] in table ["+join.getTable()+"]";
				logger.error(msg);
				return null;
			} else {
			  BeanProperty[] idProps = {idProp};
				return createImportedScalar(owner, cols[0], idProps, others);
			}
		} else {
			// embedded id
			BeanPropertyAssocOne<?> embProp = (BeanPropertyAssocOne<?>)idProp;
			BeanProperty[] embBaseProps = embProp.getTargetDescriptor().propertiesBaseScalar();
			ImportedIdSimple[] scalars = createImportedList(owner, cols, embBaseProps, others);

			return new ImportedIdEmbedded(owner, embProp, scalars);
		}
	}

	private ImportedIdSimple[] createImportedList(BeanPropertyAssoc<?> owner, TableJoinColumn[] cols, BeanProperty[] props, BeanProperty[] others) {

		ArrayList<ImportedIdSimple> list = new ArrayList<ImportedIdSimple>();

		for (int i = 0; i < cols.length; i++) {
			list.add(createImportedScalar(owner, cols[i], props, others));
		}
		
		return ImportedIdSimple.sort(list);
	}

	private ImportedIdSimple createImportedScalar(BeanPropertyAssoc<?> owner, TableJoinColumn col, BeanProperty[] props, BeanProperty[] others) {

		String matchColumn = col.getForeignDbColumn();
		String localColumn = col.getLocalDbColumn();
		
		for (int j = 0; j < props.length; j++) {
			if (props[j].getDbColumn().equalsIgnoreCase(matchColumn)) {
				return new ImportedIdSimple(owner, localColumn, props[j], j);
			}
		}

		for (int j = 0; j < others.length; j++) {
            if (others[j].getDbColumn().equalsIgnoreCase(matchColumn)) {
                return new ImportedIdSimple(owner, localColumn, others[j], j+props.length);
            }
        }
		
		String msg = "Error with the Join on ["+getFullBeanName()
			+"]. Could not find the local match for ["+matchColumn+"] "//in table["+searchTable+"]?"
			+" Perhaps an error in a @JoinColumn";
		throw new PersistenceException(msg);
	}
}
