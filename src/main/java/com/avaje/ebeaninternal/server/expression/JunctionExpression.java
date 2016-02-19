package com.avaje.ebeaninternal.server.expression;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.*;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.text.PathProperties;
import com.avaje.ebeaninternal.api.HashQueryPlanBuilder;
import com.avaje.ebeaninternal.api.ManyWhereJoins;
import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiExpressionValidation;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

/**
 * Junction implementation.
 */
abstract class JunctionExpression<T> implements Junction<T>, SpiExpression, ExpressionList<T> {

  private static final long serialVersionUID = -7422204102750462676L;

  private static final String OR = " or ";

  private static final String AND = " and ";

  static class Conjunction<T> extends JunctionExpression<T> {

    private static final long serialVersionUID = -645619859900030678L;

    Conjunction(com.avaje.ebean.Query<T> query, ExpressionList<T> parent) {
      super(false, AND, query, parent);
    }

    Conjunction(DefaultExpressionList<T> expressionList) {
      super(false, AND, expressionList);
    }
    @Override
    public SpiExpression copyForPlanKey() {
      return new Conjunction<T>(exprList.copyForPlanKey());
    }
  }

  static class Disjunction<T> extends JunctionExpression<T> {

    private static final long serialVersionUID = -8464470066692221413L;

    Disjunction(com.avaje.ebean.Query<T> query, ExpressionList<T> parent) {
      super(true, OR, query, parent);
    }

    Disjunction(DefaultExpressionList<T> expressionList) {
      super(true, OR, expressionList);
    }

    @Override
    public SpiExpression copyForPlanKey() {
      return new Disjunction<T>(exprList.copyForPlanKey());
    }
  }

  protected final DefaultExpressionList<T> exprList;

  private final String joinType;

  /**
   * If true then a disjunction which means outer joins are required.
   */
  private final boolean disjunction;

  JunctionExpression(boolean disjunction, String joinType, com.avaje.ebean.Query<T> query, ExpressionList<T> parent) {
    this.disjunction = disjunction;
    this.joinType = joinType;
    this.exprList = new DefaultExpressionList<T>(query, parent);
  }

  /**
   * Construct for copyForPlanKey.
   */
  JunctionExpression(boolean disjunction, String joinType, DefaultExpressionList<T> exprList) {
    this.disjunction = disjunction;
    this.joinType = joinType;
    this.exprList = exprList;
  }

  @Override
  public void writeElastic(ElasticExpressionContext context) throws IOException {
    throw new IllegalStateException("Not supported");
  }

  @Override
  public void containsMany(BeanDescriptor<?> desc, ManyWhereJoins manyWhereJoin) {

    List<SpiExpression> list = exprList.internalList();

    // get the current state for 'require outer joins'
    boolean parentOuterJoins = manyWhereJoin.isRequireOuterJoins();
    if (disjunction) {
      // turn on outer joins required for disjunction expressions
      manyWhereJoin.setRequireOuterJoins(true);
    }

    for (int i = 0; i < list.size(); i++) {
      list.get(i).containsMany(desc, manyWhereJoin);
    }
    if (disjunction && !parentOuterJoins) {
      // restore state to not forcing outer joins
      manyWhereJoin.setRequireOuterJoins(false);
    }
  }

  @Override
  public void validate(SpiExpressionValidation validation) {
    exprList.validate(validation);
  }

  @Override
  public Junction<T> add(Expression item) {
    SpiExpression i = (SpiExpression) item;
    exprList.add(i);
    return this;
  }

  @Override
  public Junction<T> addAll(ExpressionList<T> addList) {
    exprList.addAll(addList);
    return this;
  }

  @Override
  public void addBindValues(SpiExpressionRequest request) {

    List<SpiExpression> list = exprList.internalList();

    for (int i = 0; i < list.size(); i++) {
      SpiExpression item = list.get(i);
      item.addBindValues(request);
    }
  }

  @Override
  public void addSql(SpiExpressionRequest request) {

    List<SpiExpression> list = exprList.internalList();

    if (!list.isEmpty()) {
      request.append("(");

      for (int i = 0; i < list.size(); i++) {
        SpiExpression item = list.get(i);
        if (i > 0) {
          request.append(joinType);
        }
        item.addSql(request);
      }

      request.append(") ");
    }
  }

  @Override
  public void prepareExpression(BeanQueryRequest<?> request) {
    List<SpiExpression> list = exprList.internalList();
    for (int i = 0; i < list.size(); i++) {
      list.get(i).prepareExpression(request);
    }
  }

  /**
   * Based on Junction type and all the expression contained.
   */
  @Override
  public void queryPlanHash(HashQueryPlanBuilder builder) {
    builder.add(JunctionExpression.class).add(joinType);
    List<SpiExpression> list = exprList.internalList();
    for (int i = 0; i < list.size(); i++) {
      list.get(i).queryPlanHash(builder);
    }
  }

  @Override
  public int queryBindHash() {
    int hc = JunctionExpression.class.getName().hashCode();

    List<SpiExpression> list = exprList.internalList();
    for (int i = 0; i < list.size(); i++) {
      hc = hc * 31 + list.get(i).queryBindHash();
    }

    return hc;
  }

  @Override
  public boolean isSameByPlan(SpiExpression other) {

    if (!(other instanceof JunctionExpression)) {
      return false;
    }

    JunctionExpression that = (JunctionExpression) other;
    return joinType.equals(that.joinType)
        && exprList.isSameByPlan(that.exprList);
  }

  @Override
  public boolean isSameByBind(SpiExpression other) {
    JunctionExpression that = (JunctionExpression) other;
    return joinType.equals(that.joinType)
        && exprList.isSameByBind(that.exprList);
  }

  @Override
  public ExpressionList<T> endJunction() {
    return exprList.endJunction();
  }

  @Override
  public ExpressionList<T> allEq(Map<String, Object> propertyMap) {
    return exprList.allEq(propertyMap);
  }

  @Override
  public ExpressionList<T> and(Expression expOne, Expression expTwo) {
    return exprList.and(expOne, expTwo);
  }

  @Override
  public ExpressionList<T> between(String propertyName, Object value1, Object value2) {
    return exprList.between(propertyName, value1, value2);
  }

  @Override
  public ExpressionList<T> betweenProperties(String lowProperty, String highProperty, Object value) {
    return exprList.betweenProperties(lowProperty, highProperty, value);
  }

  @Override
  public Junction<T> conjunction() {
    return exprList.conjunction();
  }

  @Override
  public ExpressionList<T> contains(String propertyName, String value) {
    return exprList.contains(propertyName, value);
  }

  @Override
  public Junction<T> disjunction() {
    return exprList.disjunction();
  }

  @Override
  public ExpressionList<T> endsWith(String propertyName, String value) {
    return exprList.endsWith(propertyName, value);
  }

  @Override
  public ExpressionList<T> eq(String propertyName, Object value) {
    return exprList.eq(propertyName, value);
  }

  @Override
  public ExpressionList<T> exampleLike(Object example) {
    return exprList.exampleLike(example);
  }

  @Override
  public ExpressionList<T> filterMany(String prop) {
    throw new RuntimeException("filterMany not allowed on Junction expression list");
  }

  @Override
  public int delete() {
    return exprList.delete();
  }

  @Override
  public Query<T> asOf(Timestamp asOf) {
    return exprList.asOf(asOf);
  }

  @Override
  public Query<T> asDraft() {
    return exprList.asDraft();
  }

  @Override
  public Query<T> includeSoftDeletes() {
    return exprList.includeSoftDeletes();
  }

  @Override
  public List<Version<T>> findVersions() {
    return exprList.findVersions();
  }

  @Override
  public List<Version<T>> findVersionsBetween(Timestamp start, Timestamp end) {
    return exprList.findVersionsBetween(start, end);
  }

  @Override
  public Query<T> apply(PathProperties pathProperties) {
    return exprList.apply(pathProperties);
  }

  @Override
  public FutureIds<T> findFutureIds() {
    return exprList.findFutureIds();
  }

  @Override
  public FutureList<T> findFutureList() {
    return exprList.findFutureList();
  }

  @Override
  public FutureRowCount<T> findFutureRowCount() {
    return exprList.findFutureRowCount();
  }

  @Override
  public List<Object> findIds() {
    return exprList.findIds();
  }

  @Override
  public void findEach(QueryEachConsumer<T> consumer) {
    exprList.findEach(consumer);
  }

  @Override
  public void findEachWhile(QueryEachWhileConsumer<T> consumer) {
    exprList.findEachWhile(consumer);
  }

  @Override
  public QueryIterator<T> findIterate() {
    return exprList.findIterate();
  }

  @Override
  public List<T> findList() {
    return exprList.findList();
  }

  @Override
  public Map<?, T> findMap() {
    return exprList.findMap();
  }

  @Override
  public <K> Map<K, T> findMap(String keyProperty, Class<K> keyType) {
    return exprList.findMap(keyProperty, keyType);
  }

  @Override
  public PagedList<T> findPagedList(int pageIndex, int pageSize) {
    return exprList.findPagedList(pageIndex, pageSize);
  }

  @Override
  public PagedList<T> findPagedList() {
    return exprList.findPagedList();
  }

  @Override
  public int findRowCount() {
    return exprList.findRowCount();
  }

  @Override
  public Set<T> findSet() {
    return exprList.findSet();
  }

  @Override
  public T findUnique() {
    return exprList.findUnique();
  }

  /**
   * Path exists - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonExists(String propertyName, String path) {
    return exprList.jsonExists(propertyName, path);
  }

  /**
   * Path does not exist - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonNotExists(String propertyName, String path){
    return exprList.jsonNotExists(propertyName, path);
  }

  /**
   * Equal to - for the value at the given path in the JSON document.
   */
  @Override
  public ExpressionList<T> jsonEqualTo(String propertyName, String path, Object value){
    return exprList.jsonEqualTo(propertyName, path, value);
  }

  /**
   * Not Equal to - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonNotEqualTo(String propertyName, String path, Object val){
    return exprList.jsonNotEqualTo(propertyName, path, val);
  }

  /**
   * Greater than - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonGreaterThan(String propertyName, String path, Object val){
    return exprList.jsonGreaterThan(propertyName, path, val);
  }

  /**
   * Greater than or equal to - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonGreaterOrEqual(String propertyName, String path, Object val){
    return exprList.jsonGreaterOrEqual(propertyName, path, val);
  }

  /**
   * Less than - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonLessThan(String propertyName, String path, Object val){
    return exprList.jsonLessThan(propertyName, path, val);
  }

  /**
   * Less than or equal to - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonLessOrEqualTo(String propertyName, String path, Object val){
    return exprList.jsonLessOrEqualTo(propertyName, path, val);
  }

  /**
   * Between - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonBetween(String propertyName, String path, Object lowerValue, Object upperValue){
    return exprList.jsonBetween(propertyName, path, lowerValue, upperValue);
  }

  @Override
  public ExpressionList<T> ge(String propertyName, Object value) {
    return exprList.ge(propertyName, value);
  }

  @Override
  public ExpressionList<T> gt(String propertyName, Object value) {
    return exprList.gt(propertyName, value);
  }

  @Override
  public ExpressionList<T> having() {
    throw new RuntimeException("having() not allowed on Junction expression list");
  }

  @Override
  public ExpressionList<T> icontains(String propertyName, String value) {
    return exprList.icontains(propertyName, value);
  }

  @Override
  public ExpressionList<T> idEq(Object value) {
    return exprList.idEq(value);
  }

  @Override
  public ExpressionList<T> idIn(List<?> idValues) {
    return exprList.idIn(idValues);
  }

  @Override
  public ExpressionList<T> iendsWith(String propertyName, String value) {
    return exprList.iendsWith(propertyName, value);
  }

  @Override
  public ExpressionList<T> ieq(String propertyName, String value) {
    return exprList.ieq(propertyName, value);
  }

  @Override
  public ExpressionList<T> iexampleLike(Object example) {
    return exprList.iexampleLike(example);
  }

  @Override
  public ExpressionList<T> ilike(String propertyName, String value) {
    return exprList.ilike(propertyName, value);
  }

  @Override
  public ExpressionList<T> in(String propertyName, Collection<?> values) {
    return exprList.in(propertyName, values);
  }

  @Override
  public ExpressionList<T> in(String propertyName, Object... values) {
    return exprList.in(propertyName, values);
  }

  @Override
  public ExpressionList<T> in(String propertyName, com.avaje.ebean.Query<?> subQuery) {
    return exprList.in(propertyName, subQuery);
  }

  @Override
  public ExpressionList<T> notIn(String propertyName, Collection<?> values) {
    return exprList.notIn(propertyName, values);
  }

  @Override
  public ExpressionList<T> notIn(String propertyName, Object... values) {
    return exprList.notIn(propertyName, values);
  }

  @Override
  public ExpressionList<T> notIn(String propertyName, com.avaje.ebean.Query<?> subQuery) {
    return exprList.notIn(propertyName, subQuery);
  }

  @Override
  public ExpressionList<T> exists(Query<?> subQuery) {
    return exprList.exists(subQuery);
  }

  @Override
  public ExpressionList<T> notExists(Query<?> subQuery) {
    return exprList.exists(subQuery);
  }

  @Override
  public ExpressionList<T> isNotNull(String propertyName) {
    return exprList.isNotNull(propertyName);
  }

  @Override
  public ExpressionList<T> isNull(String propertyName) {
    return exprList.isNull(propertyName);
  }

  @Override
  public ExpressionList<T> istartsWith(String propertyName, String value) {
    return exprList.istartsWith(propertyName, value);
  }

  @Override
  public ExpressionList<T> le(String propertyName, Object value) {
    return exprList.le(propertyName, value);
  }

  @Override
  public ExpressionList<T> like(String propertyName, String value) {
    return exprList.like(propertyName, value);
  }

  @Override
  public ExpressionList<T> lt(String propertyName, Object value) {
    return exprList.lt(propertyName, value);
  }

  @Override
  public ExpressionList<T> ne(String propertyName, Object value) {
    return exprList.ne(propertyName, value);
  }

  @Override
  public ExpressionList<T> not(Expression exp) {
    return exprList.not(exp);
  }

  @Override
  public ExpressionList<T> or(Expression expOne, Expression expTwo) {
    return exprList.or(expOne, expTwo);
  }

  @Override
  public OrderBy<T> order() {
    return exprList.order();
  }

  @Override
  public com.avaje.ebean.Query<T> order(String orderByClause) {
    return exprList.order(orderByClause);
  }

  @Override
  public OrderBy<T> orderBy() {
    return exprList.orderBy();
  }

  @Override
  public com.avaje.ebean.Query<T> orderBy(String orderBy) {
    return exprList.orderBy(orderBy);
  }

  @Override
  public com.avaje.ebean.Query<T> query() {
    return exprList.query();
  }

  @Override
  public ExpressionList<T> raw(String raw, Object value) {
    return exprList.raw(raw, value);
  }

  @Override
  public ExpressionList<T> raw(String raw, Object... values) {
    return exprList.raw(raw, values);
  }

  @Override
  public ExpressionList<T> raw(String raw) {
    return exprList.raw(raw);
  }

  @Override
  public com.avaje.ebean.Query<T> select(String properties) {
    return exprList.select(properties);
  }

  @Override
  public Query<T> setDistinct(boolean distinct) {
    return exprList.setDistinct(distinct);
  }

  @Override
  public com.avaje.ebean.Query<T> setFirstRow(int firstRow) {
    return exprList.setFirstRow(firstRow);
  }

  @Override
  public com.avaje.ebean.Query<T> setMapKey(String mapKey) {
    return exprList.setMapKey(mapKey);
  }

  @Override
  public com.avaje.ebean.Query<T> setMaxRows(int maxRows) {
    return exprList.setMaxRows(maxRows);
  }

  @Override
  public com.avaje.ebean.Query<T> setOrderBy(String orderBy) {
    return exprList.setOrderBy(orderBy);
  }

  @Override
  public com.avaje.ebean.Query<T> setUseCache(boolean useCache) {
    return exprList.setUseCache(useCache);
  }

  @Override
  public Query<T> setUseQueryCache(boolean useCache) {
    return exprList.setUseQueryCache(useCache);
  }

  @Override
  public ExpressionList<T> startsWith(String propertyName, String value) {
    return exprList.startsWith(propertyName, value);
  }

  @Override
  public ExpressionList<T> where() {
    return exprList.where();
  }

}
