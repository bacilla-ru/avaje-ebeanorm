package com.avaje.ebeaninternal.server.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.HashQueryPlanBuilder;
import com.avaje.ebeaninternal.api.ManyWhereJoins;
import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiExpressionValidation;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

import java.io.IOException;

/**
 * Effectively an expression that has no effect.
 */
class NoopExpression implements SpiExpression {

  protected static final NoopExpression INSTANCE = new NoopExpression();

  @Override
  public SpiExpression copyForPlanKey() {
    return this;
  }

  @Override
  public void writeElastic(ElasticExpressionContext context) throws IOException {
  }

  @Override
  public void containsMany(BeanDescriptor<?> desc, ManyWhereJoins whereManyJoins) {
    // nothing to do
  }

  @Override
  public void validate(SpiExpressionValidation validation) {
    // always valid
  }

  @Override
  public void prepareExpression(BeanQueryRequest<?> request) {
    // do nothing
  }

  @Override
  public void queryPlanHash(HashQueryPlanBuilder builder) {
    builder.add(NoopExpression.class);
  }

  @Override
  public int queryBindHash() {
    // no bind values
    return 0;
  }

  @Override
  public void addSql(SpiExpressionRequest request) {
    request.append("1=1");
  }

  @Override
  public void addBindValues(SpiExpressionRequest request) {
    // nothing to do
  }

  @Override
  public boolean isSameByPlan(SpiExpression other) {
    return other instanceof NoopExpression;
  }

  @Override
  public boolean isSameByBind(SpiExpression other) {
    return true;
  }
}
