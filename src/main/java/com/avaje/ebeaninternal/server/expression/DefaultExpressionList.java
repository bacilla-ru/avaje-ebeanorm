package com.avaje.ebeaninternal.server.expression;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
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
import com.avaje.ebeaninternal.api.SpiExpressionList;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiExpressionValidation;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Default implementation of ExpressionList.
 */
public class DefaultExpressionList<T> implements SpiExpressionList<T> {

  private static final long serialVersionUID = -6992345500247035947L;

  protected final List<SpiExpression> list;

  protected final Query<T> query;

  protected final ExpressionList<T> parentExprList;

  protected transient ExpressionFactory expr;

  private final String listAndStart;
  private final String listAndEnd;
  private final String listAndJoin;

  public DefaultExpressionList(Query<T> query, ExpressionList<T> parentExprList) {
    this(query, query.getExpressionFactory(), parentExprList);
  }

  public DefaultExpressionList(Query<T> query, ExpressionFactory expr, ExpressionList<T> parentExprList) {
    this(query, expr, parentExprList, new ArrayList<SpiExpression>());
  }

  protected DefaultExpressionList(Query<T> query, ExpressionFactory expr, ExpressionList<T> parentExprList, List<SpiExpression> list) {
    this.list = list;
    this.query = query;
    this.expr = expr;
    this.parentExprList = parentExprList;

    this.listAndStart = "";
    this.listAndEnd = "";
    this.listAndJoin = " and ";
  }

  private DefaultExpressionList() {
    this(null, null, null, new ArrayList<SpiExpression>());
  }

  @Override
  public void writeElastic(ElasticExpressionContext context) throws IOException {

    JsonGenerator json = context.json();
    json.writeStartObject();
    json.writeFieldName("bool");
    json.writeStartObject();
    json.writeFieldName("must");
    json.writeStartArray();
    for (int i = 0; i < list.size(); i++) {
      list.get(i).writeElastic(context);
    }
    json.writeEndArray();
    json.writeEndObject();
    json.writeEndObject();
  }

  @Override
  public SpiExpressionList<?> trimPath(int prefixTrim) {
    throw new RuntimeException("Only allowed on FilterExpressionList");
  }

  public List<SpiExpression> internalList() {
    return list;
  }

  /**
   * Return a copy of the expression list.
   * <p>
   * Each of the expressions are expected to be immutable and safe to reference.
   * </p>
   */
  public DefaultExpressionList<T> copy(Query<T> query) {
    DefaultExpressionList<T> copy = new DefaultExpressionList<T>(query, expr, null);
    copy.list.addAll(list);
    return copy;
  }

  public DefaultExpressionList<T> copyForPlanKey() {
    DefaultExpressionList<T> copy = new DefaultExpressionList<T>();
    for (int i = 0; i < list.size(); i++) {
      copy.list.add(list.get(i).copyForPlanKey());
    }
    return copy;
  }

  /**
   * Return true if one of the expressions is related to a Many property.
   */
  @Override
  public void containsMany(BeanDescriptor<?> desc, ManyWhereJoins whereManyJoins) {

    for (int i = 0; i < list.size(); i++) {
      list.get(i).containsMany(desc, whereManyJoins);
    }
  }

  @Override
  public void validate(SpiExpressionValidation validation) {
    for (int i = 0; i < list.size(); i++) {
      list.get(i).validate(validation);
    }
  }

  @Override
  public ExpressionList<T> endJunction() {
    return parentExprList == null ? this : parentExprList;
  }

  @Override
  public Query<T> query() {
    return query;
  }

  @Override
  public Query<T> asOf(Timestamp asOf) {
    return query.asOf(asOf);
  }

  @Override
  public Query<T> asDraft() {
    return query.asDraft();
  }

  @Override
  public Query<T> includeSoftDeletes() {
    return query.includeSoftDeletes();
  }

  @Override
  public List<Version<T>> findVersions() {
    return query.findVersions();
  }

  @Override
  public List<Version<T>> findVersionsBetween(Timestamp start, Timestamp end) {
    return query.findVersionsBetween(start, end);
  }

  @Override
  public ExpressionList<T> where() {
    return query.where();
  }

  @Override
  public OrderBy<T> order() {
    return query.order();
  }

  @Override
  public OrderBy<T> orderBy() {
    return query.order();
  }

  @Override
  public Query<T> order(String orderByClause) {
    return query.order(orderByClause);
  }

  @Override
  public Query<T> orderBy(String orderBy) {
    return query.order(orderBy);
  }

  @Override
  public Query<T> setOrderBy(String orderBy) {
    return query.order(orderBy);
  }

  @Override
  public Query<T> apply(PathProperties pathProperties) {
    return query.apply(pathProperties);
  }

  @Override
  public int delete() {
    return query.delete();
  }

  @Override
  public FutureIds<T> findFutureIds() {
    return query.findFutureIds();
  }

  @Override
  public FutureRowCount<T> findFutureRowCount() {
    return query.findFutureRowCount();
  }

  @Override
  public FutureList<T> findFutureList() {
    return query.findFutureList();
  }

  @Override
  public PagedList<T> findPagedList(int pageIndex, int pageSize) {
    return query.findPagedList(pageIndex, pageSize);
  }

  @Override
  public PagedList<T> findPagedList() {
    return query.findPagedList();
  }

  @Override
  public int findRowCount() {
    return query.findRowCount();
  }

  @Override
  public List<Object> findIds() {
    return query.findIds();
  }

  @Override
  public void findEach(QueryEachConsumer<T> consumer) {
    query.findEach(consumer);
  }

  @Override
  public void findEachWhile(QueryEachWhileConsumer<T> consumer) {
    query.findEachWhile(consumer);
  }

  @Override
  public QueryIterator<T> findIterate() {
    return query.findIterate();
  }

  @Override
  public List<T> findList() {
    return query.findList();
  }

  @Override
  public Set<T> findSet() {
    return query.findSet();
  }

  @Override
  public Map<?, T> findMap() {
    return query.findMap();
  }

  @Override
  public <K> Map<K, T> findMap(String keyProperty, Class<K> keyType) {
    return query.findMap(keyProperty, keyType);
  }

  @Override
  public T findUnique() {
    return query.findUnique();
  }

  @Override
  public ExpressionList<T> filterMany(String prop) {
    return query.filterMany(prop);
  }

  @Override
  public Query<T> select(String fetchProperties) {
    return query.select(fetchProperties);
  }

  @Override
  public Query<T> setDistinct(boolean distinct) {
    return query.setDistinct(distinct);
  }

  @Override
  public Query<T> setFirstRow(int firstRow) {
    return query.setFirstRow(firstRow);
  }

  @Override
  public Query<T> setMaxRows(int maxRows) {
    return query.setMaxRows(maxRows);
  }

  @Override
  public Query<T> setMapKey(String mapKey) {
    return query.setMapKey(mapKey);
  }

  @Override
  public Query<T> setUseCache(boolean useCache) {
    return query.setUseCache(useCache);
  }

  @Override
  public Query<T> setUseQueryCache(boolean useCache) {
    return query.setUseQueryCache(useCache);
  }

  @Override
  public ExpressionList<T> having() {
    return query.having();
  }

  @Override
  public ExpressionList<T> add(Expression expr) {
    list.add((SpiExpression) expr);
    return this;
  }

  @Override
  public ExpressionList<T> addAll(ExpressionList<T> exprList) {
    SpiExpressionList<T> spiList = (SpiExpressionList<T>) exprList;
    list.addAll(spiList.getUnderlyingList());
    return this;
  }

  @Override
  public List<SpiExpression> getUnderlyingList() {
    return list;
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public void addSql(SpiExpressionRequest request) {

    request.append(listAndStart);
    for (int i = 0, size = list.size(); i < size; i++) {
      SpiExpression expression = list.get(i);
      if (i > 0) {
        request.append(listAndJoin);
      }
      expression.addSql(request);
    }
    request.append(listAndEnd);
  }

  @Override
  public void addBindValues(SpiExpressionRequest request) {
    for (int i = 0, size = list.size(); i < size; i++) {
      list.get(i).addBindValues(request);
    }
  }

  @Override
  public void prepareExpression(BeanQueryRequest<?> request) {
    for (int i = 0, size = list.size(); i < size; i++) {
      list.get(i).prepareExpression(request);
    }
  }

  /**
   * Calculate a hash based on the expressions but excluding the actual bind
   * values.
   */
  @Override
  public void queryPlanHash(HashQueryPlanBuilder builder) {
    builder.add(DefaultExpressionList.class);
    for (int i = 0, size = list.size(); i < size; i++) {
      list.get(i).queryPlanHash(builder);
    }
  }

  /**
   * Calculate a hash based on the expressions.
   */
  @Override
  public int queryBindHash() {
    int hash = DefaultExpressionList.class.getName().hashCode();
    for (int i = 0, size = list.size(); i < size; i++) {
      hash = hash * 31 + list.get(i).queryBindHash();
    }
    return hash;
  }

  @Override
  public boolean isSameByPlan(SpiExpression other) {
    if (!(other instanceof DefaultExpressionList)) {
      return false;
    }

    DefaultExpressionList<?> that = (DefaultExpressionList<?>)other;
    if (list.size() != that.list.size()) {
      return false;
    }
    for (int i = 0, size = list.size(); i < size; i++) {
      if (!list.get(i).isSameByPlan(that.list.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isSameByBind(SpiExpression other) {
    DefaultExpressionList<?> that = (DefaultExpressionList<?>)other;
    if (list.size() != that.list.size()) {
      return false;
    }
    for (int i = 0, size = list.size(); i < size; i++) {
      if (!list.get(i).isSameByBind(that.list.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Path exists - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonExists(String propertyName, String path) {
    add(expr.jsonExists(propertyName, path));
    return this;
  }

  /**
   * Path does not exist - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonNotExists(String propertyName, String path){
    add(expr.jsonNotExists(propertyName, path));
    return this;
  }
  
  /**
   * Equal to expression for the value at the given path in the JSON document.
   */
  @Override
  public ExpressionList<T> jsonEqualTo(String propertyName, String path, Object value){
    add(expr.jsonEqualTo(propertyName, path, value));
    return this;
  }

  /**
   * Not Equal to - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonNotEqualTo(String propertyName, String path, Object val){
    add(expr.jsonNotEqualTo(propertyName, path, val));
    return this;
  }

  /**
   * Greater than - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonGreaterThan(String propertyName, String path, Object val){
    add(expr.jsonGreaterThan(propertyName, path, val));
    return this;
  }

  /**
   * Greater than or equal to - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonGreaterOrEqual(String propertyName, String path, Object val){
    add(expr.jsonGreaterOrEqual(propertyName, path, val));
    return this;
  }

  /**
   * Less than - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonLessThan(String propertyName, String path, Object val){
    add(expr.jsonLessThan(propertyName, path, val));
    return this;
  }

  /**
   * Less than or equal to - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonLessOrEqualTo(String propertyName, String path, Object val){
    add(expr.jsonLessOrEqualTo(propertyName, path, val));
    return this;
  }

  /**
   * Between - for the given path in a JSON document.
   */
  @Override
  public ExpressionList<T> jsonBetween(String propertyName, String path, Object lowerValue, Object upperValue){
    add(expr.jsonBetween(propertyName, path, lowerValue, upperValue));
    return this;
  }

  @Override
  public ExpressionList<T> eq(String propertyName, Object value) {
    add(expr.eq(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> ieq(String propertyName, String value) {
    add(expr.ieq(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> ne(String propertyName, Object value) {
    add(expr.ne(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> allEq(Map<String, Object> propertyMap) {
    add(expr.allEq(propertyMap));
    return this;
  }

  @Override
  public ExpressionList<T> and(Expression expOne, Expression expTwo) {
    add(expr.and(expOne, expTwo));
    return this;
  }

  @Override
  public ExpressionList<T> between(String propertyName, Object value1, Object value2) {
    add(expr.between(propertyName, value1, value2));
    return this;
  }

  @Override
  public ExpressionList<T> betweenProperties(String lowProperty, String highProperty, Object value) {
    add(expr.betweenProperties(lowProperty, highProperty, value));
    return this;
  }

  @Override
  public Junction<T> conjunction() {
    Junction<T> conjunction = expr.conjunction(query, this);
    add(conjunction);
    return conjunction;
  }

  @Override
  public ExpressionList<T> contains(String propertyName, String value) {
    add(expr.contains(propertyName, value));
    return this;
  }

  @Override
  public Junction<T> disjunction() {
    Junction<T> disjunction = expr.disjunction(query, this);
    add(disjunction);
    return disjunction;
  }

  @Override
  public ExpressionList<T> endsWith(String propertyName, String value) {
    add(expr.endsWith(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> ge(String propertyName, Object value) {
    add(expr.ge(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> gt(String propertyName, Object value) {
    add(expr.gt(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> icontains(String propertyName, String value) {
    add(expr.icontains(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> idIn(List<?> idList) {
    add(expr.idIn(idList));
    return this;
  }

  @Override
  public ExpressionList<T> idEq(Object value) {
    if (query != null && parentExprList == null) {
      query.setId(value);
    } else {
      add(expr.idEq(value));
    }
    return this;
  }

  @Override
  public ExpressionList<T> iendsWith(String propertyName, String value) {
    add(expr.iendsWith(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> ilike(String propertyName, String value) {
    add(expr.ilike(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> in(String propertyName, Query<?> subQuery) {
    add(expr.in(propertyName, subQuery));
    return this;
  }

  @Override
  public ExpressionList<T> in(String propertyName, Collection<?> values) {
    add(expr.in(propertyName, values));
    return this;
  }

  @Override
  public ExpressionList<T> in(String propertyName, Object... values) {
    add(expr.in(propertyName, values));
    return this;
  }

  @Override
  public ExpressionList<T> notIn(String propertyName, Object... values) {
    add(expr.notIn(propertyName, values));
    return this;
  }

  @Override
  public ExpressionList<T> notIn(String propertyName, Collection<?> values) {
    add(expr.notIn(propertyName, values));
    return this;
  }

  @Override
  public ExpressionList<T> notIn(String propertyName, Query<?> subQuery) {
    add(expr.notIn(propertyName, subQuery));
    return this;
  }

  @Override
  public ExpressionList<T> exists(Query<?> subQuery) {
    add(expr.exists(subQuery));
    return this;
  }

  @Override
  public ExpressionList<T> notExists(Query<?> subQuery) {
    add(expr.notExists(subQuery));
    return this;
  }

  @Override
  public ExpressionList<T> isNotNull(String propertyName) {
    add(expr.isNotNull(propertyName));
    return this;
  }

  @Override
  public ExpressionList<T> isNull(String propertyName) {
    add(expr.isNull(propertyName));
    return this;
  }

  @Override
  public ExpressionList<T> istartsWith(String propertyName, String value) {
    add(expr.istartsWith(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> le(String propertyName, Object value) {
    add(expr.le(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> exampleLike(Object example) {
    add(expr.exampleLike(example));
    return this;
  }

  @Override
  public ExpressionList<T> iexampleLike(Object example) {
    add(expr.iexampleLike(example));
    return this;
  }

  @Override
  public ExpressionList<T> like(String propertyName, String value) {
    add(expr.like(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> lt(String propertyName, Object value) {
    add(expr.lt(propertyName, value));
    return this;
  }

  @Override
  public ExpressionList<T> not(Expression exp) {
    add(expr.not(exp));
    return this;
  }

  @Override
  public ExpressionList<T> or(Expression expOne, Expression expTwo) {
    add(expr.or(expOne, expTwo));
    return this;
  }

  @Override
  public ExpressionList<T> raw(String raw, Object value) {
    add(expr.raw(raw, value));
    return this;
  }

  @Override
  public ExpressionList<T> raw(String raw, Object... values) {
    add(expr.raw(raw, values));
    return this;
  }

  @Override
  public ExpressionList<T> raw(String raw) {
    add(expr.raw(raw));
    return this;
  }

  @Override
  public ExpressionList<T> startsWith(String propertyName, String value) {
    add(expr.startsWith(propertyName, value));
    return this;
  }

}
