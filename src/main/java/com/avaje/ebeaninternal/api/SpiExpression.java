package com.avaje.ebeaninternal.api;

import com.avaje.ebean.Expression;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.expression.ElasticExpressionContext;

import java.io.IOException;


/**
 * An expression that becomes part of a Where clause or Having clause.
 */
public interface SpiExpression extends Expression {

  /**
   * Write the expression as an elastic search expression.
   */
  void writeElastic(ElasticExpressionContext context) throws IOException;

  /**
   * Process "Many" properties populating ManyWhereJoins.
   * <p>
   * Predicates on Many properties require an extra independent join clause.
   * </p>
   */
	void containsMany(BeanDescriptor<?> desc, ManyWhereJoins whereManyJoins);

  /**
   * Prepare the expression. For example, compile sub-query expressions etc.
   */
  void prepareExpression(BeanQueryRequest<?> request);

  /**
	 * Calculate a hash value used to identify a query for AutoTune tuning.
	 * <p>
	 * That is, if the hash changes then the query will be considered different
	 * from an AutoTune perspective and get different tuning.
	 * </p>
	 */
	void queryPlanHash(HashQueryPlanBuilder builder);

	/**
	 * Return the hash value for the values that will be bound.
	 */
	int queryBindHash();

  /**
   * Return true if the expression is the same without taking into account bind values.
   */
  boolean isSameByPlan(SpiExpression other);

  /**
   * Return true if the expression is the same with respect to bind values.
   */
  boolean isSameByBind(SpiExpression other);

	/**
	 * Add some sql to the query.
	 * <p>
	 * This will contain ? as a place holder for each associated bind values.
	 * </p>
	 * <p>
	 * The 'sql' added to the query can contain object property names rather
	 * than db tables and columns. This 'sql' is later parsed converting the
	 * logical property names to their full database column names.
	 * </p>
	 * @param request
	 *            the associated request.
	 */
	void addSql(SpiExpressionRequest request);

	/**
	 * Add the parameter values to be set against query. For each ? place holder
	 * there should be a corresponding value that is added to the bindList.
	 *
	 * @param request
	 *            the associated request.
	 */
	void addBindValues(SpiExpressionRequest request);

  /**
   * Validate all the properties/paths associated with this expression.
   */
  void validate(SpiExpressionValidation validation);

  /**
   * Return a copy of the expression for use in the query plan key.
   */
  SpiExpression copyForPlanKey();

}
