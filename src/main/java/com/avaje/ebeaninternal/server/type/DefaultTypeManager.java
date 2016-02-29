package com.avaje.ebeaninternal.server.type;

import com.avaje.ebean.annotation.DbEnumType;
import com.avaje.ebean.annotation.DbEnumValue;
import com.avaje.ebean.annotation.EnumMapping;
import com.avaje.ebean.annotation.EnumValue;
import com.avaje.ebean.config.*;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.dbplatform.DbType;
import com.avaje.ebeaninternal.server.core.BootupClasses;
import com.avaje.ebeaninternal.server.lib.util.StringHelper;
import com.avaje.ebeaninternal.server.type.reflect.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.*;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.*;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of TypeManager.
 * <p>
 * Manages the list of ScalarType that is available.
 * </p>
 */
public final class DefaultTypeManager implements TypeManager, KnownImmutable {

  private static final Logger logger = LoggerFactory.getLogger(DefaultTypeManager.class);

  private final ConcurrentHashMap<Class<?>, CtCompoundType<?>> compoundTypeMap;

  private final ConcurrentHashMap<Class<?>, ScalarType<?>> typeMap;

  private final ConcurrentHashMap<Integer, ScalarType<?>> nativeMap;

  private final DefaultTypeFactory extraTypeFactory;

  private final ScalarTypeJsonMap JSON_MAP_CLOB = new ScalarTypeJsonMap.Clob();

  private final ScalarTypeJsonMap jsonMapClob = JSON_MAP_CLOB;
  private final ScalarTypeJsonMap jsonMapBlob = new ScalarTypeJsonMap.Blob();
  private final ScalarTypeJsonMap jsonMapVarchar = new ScalarTypeJsonMap.Varchar();
  private final ScalarTypeJsonMap jsonMapJson;
  private final ScalarTypeJsonMap jsonMapJsonb;

  private final ScalarTypeFile fileType = new ScalarTypeFile();

  private final ScalarType<?> charType = new ScalarTypeChar();

  private final ScalarType<?> charArrayType = new ScalarTypeCharArray();

  private final ScalarType<?> longVarcharType = new ScalarTypeLongVarchar();

  private final ScalarType<?> clobType = new ScalarTypeClob();

  private final ScalarType<?> byteType = new ScalarTypeByte();

  private final ScalarType<?> binaryType = new ScalarTypeBytesBinary();

  private final ScalarType<?> blobType = new ScalarTypeBytesBlob();

  private final ScalarType<?> varbinaryType = new ScalarTypeBytesVarbinary();

  private final ScalarType<?> longVarbinaryType = new ScalarTypeBytesLongVarbinary();

  private final ScalarType<?> shortType = new ScalarTypeShort();

  private final ScalarType<?> integerType = new ScalarTypeInteger();

  private final ScalarType<?> longType = new ScalarTypeLong();

  private final ScalarType<?> doubleType = new ScalarTypeDouble();

  private final ScalarType<?> floatType = new ScalarTypeFloat();

  private final ScalarType<?> bigDecimalType = new ScalarTypeBigDecimal();

  private final ScalarType<?> timeType = new ScalarTypeTime();

  private final ScalarType<?> dateType = new ScalarTypeDate();

  private final ScalarType<?> inetAddressType = new ScalarTypeInetAddress();
  private final ScalarType<?> urlType = new ScalarTypeURL();
  private final ScalarType<?> uriType = new ScalarTypeURI();
  private final ScalarType<?> localeType = new ScalarTypeLocale();
  private final ScalarType<?> currencyType = new ScalarTypeCurrency();
  private final ScalarType<?> timeZoneType = new ScalarTypeTimeZone();

  private final ScalarType<?> stringType = new ScalarTypeString();

  private final ScalarType<?> classType = new ScalarTypeClass();


  private final List<ScalarType<?>> customScalarTypes = new ArrayList<ScalarType<?>>();

  private final CheckImmutable checkImmutable;

  private final ImmutableMetaFactory immutableMetaFactory = new ImmutableMetaFactory();

  private final ReflectionBasedTypeBuilder reflectScalarBuilder;

  private final JsonConfig.DateTime jsonDateTime;

  private final boolean objectMapperPresent;

  // OPTIONAL ScalarTypes registered if Jackson/JsonNode is in the classpath

  /**
   * Jackson's JsonNode storage to Clob.
   */
  private ScalarType<?> jsonNodeClob;
  /**
   * Jackson's JsonNode storage to Blob.
   */
  private ScalarType<?> jsonNodeBlob;
  /**
   * Jackson's JsonNode storage to Varchar.
   */
  private ScalarType<?> jsonNodeVarchar;
  /**
   * Jackson's JsonNode storage to Postgres JSON or Clob.
   */
  private ScalarType<?> jsonNodeJson;
  /**
   * Jackson's JsonNode storage to Postgres JSONB or Clob.
   */
  private ScalarType<?> jsonNodeJsonb;

  /**
   * Create the DefaultTypeManager.
   */
  public DefaultTypeManager(ServerConfig config, BootupClasses bootupClasses) {

    this.jsonDateTime = config.getJsonDateTime();
    this.checkImmutable = new CheckImmutable(this);
    this.reflectScalarBuilder = new ReflectionBasedTypeBuilder(this);

    this.compoundTypeMap = new ConcurrentHashMap<Class<?>, CtCompoundType<?>>();
    this.typeMap = new ConcurrentHashMap<Class<?>, ScalarType<?>>();
    this.nativeMap = new ConcurrentHashMap<Integer, ScalarType<?>>();

    this.objectMapperPresent = config.getClassLoadConfig().isJacksonObjectMapperPresent();

    this.extraTypeFactory = new DefaultTypeFactory(config);

    initialiseStandard(jsonDateTime, config);
    initialiseJavaTimeTypes(jsonDateTime, config);
    initialiseJodaTypes(jsonDateTime, config);
    initialiseJacksonTypes(config);

    if (isPostgres(config.getDatabasePlatform())) {
      // Postgres has special DB types for JSON/JSONB
      this.jsonMapJson = new ScalarTypeJsonMapPostgres.JSON();
      this.jsonMapJsonb = new ScalarTypeJsonMapPostgres.JSONB();
    } else {
      this.jsonMapJson = JSON_MAP_CLOB;
      this.jsonMapJsonb = JSON_MAP_CLOB;
    }

    if (bootupClasses != null) {
      initialiseCustomScalarTypes(jsonDateTime, bootupClasses, config);
      initialiseScalarConverters(bootupClasses);
      initialiseCompoundTypes(bootupClasses);
    }
  }

  private boolean isPostgres(DatabasePlatform databasePlatform) {
    return databasePlatform.getName().toLowerCase().startsWith("postgre");
  }

  public boolean isKnownImmutable(Class<?> cls) {

    if (cls == null) {
      // superclass from an interface
      return true;
    }

    if (cls.isPrimitive() || Object.class.equals(cls)) {
      return true;
    }

    ScalarDataReader<?> scalarDataReader = getScalarDataReader(cls);
    return scalarDataReader != null;
  }

  public CheckImmutableResponse checkImmutable(Class<?> cls) {
    return checkImmutable.checkImmutable(cls);
  }

  private ScalarType<?> register(ScalarType<?> st) {
    add(st);
    logger.debug("Registering ScalarType for " + st.getType() + " implemented using reflection");
    return st;
  }

  public ScalarDataReader<?> recursiveCreateScalarDataReader(Class<?> cls) {

    ScalarDataReader<?> scalarReader = getScalarDataReader(cls);
    if (scalarReader != null) {
      return scalarReader;
    }

    ImmutableMeta meta = immutableMetaFactory.createImmutableMeta(cls);

    if (!meta.isCompoundType()) {
      return register(reflectScalarBuilder.buildScalarType(meta));
    }

    ReflectionBasedCompoundType compoundType = reflectScalarBuilder.buildCompound(meta);
    Class<?> compoundTypeClass = compoundType.getCompoundType();

    return createCompoundScalarDataReader(compoundTypeClass, compoundType, " using reflection");

  }

  public ScalarType<?> recursiveCreateScalarTypes(Class<?> cls) {

    ScalarType<?> scalarType = getScalarType(cls);
    if (scalarType != null) {
      return scalarType;
    }

    ImmutableMeta meta = immutableMetaFactory.createImmutableMeta(cls);

    if (!meta.isCompoundType()) {
      return register(reflectScalarBuilder.buildScalarType(meta));
    }

    throw new RuntimeException("Not allowed compound types here");

  }

  /**
   * Register a custom ScalarType.
   */
  public void add(ScalarType<?> scalarType) {
    typeMap.put(scalarType.getType(), scalarType);
    logAdd(scalarType);
  }

  protected void logAdd(ScalarType<?> scalarType) {
    if (logger.isDebugEnabled()) {
      String msg = "ScalarType register [" + scalarType.getClass().getName() + "]";
      msg += " for [" + scalarType.getType().getName() + "]";
      logger.debug(msg);
    }
  }

  public CtCompoundType<?> getCompoundType(Class<?> type) {
    return compoundTypeMap.get(type);
  }

  /**
   * Return the ScalarType for the given jdbc type as per java.sql.Types.
   */
  public ScalarType<?> getScalarType(int jdbcType) {
    return nativeMap.get(jdbcType);
  }

  /**
   * This can return null if no matching ScalarType is found.
   */
  @SuppressWarnings("unchecked")
  public <T> ScalarType<T> getScalarType(Class<T> type) {
    ScalarType<T> found = (ScalarType<T>) typeMap.get(type);
    if (found == null) {
      if (type.getName().equals("org.joda.time.LocalTime")) {
        throw new IllegalStateException(
            "ScalarType of Joda LocalTime not defined. You need to set ServerConfig.jodaLocalTimeMode to"
                + " either 'normal' or 'utc'.  UTC is the old mode using UTC timezone but local time zone is now preferred as 'normal' mode.");
      }
    }
    return found;
  }

  public ScalarDataReader<?> getScalarDataReader(Class<?> propertyType, int sqlType) {

    if (sqlType == 0) {
      return recursiveCreateScalarDataReader(propertyType);
    }

    for (int i = 0; i < customScalarTypes.size(); i++) {
      ScalarType<?> customScalarType = customScalarTypes.get(i);

      if (sqlType == customScalarType.getJdbcType() && (propertyType.equals(customScalarType.getType()))) {

        return customScalarType;
      }
    }

    String msg = "Unable to find a custom ScalarType with type [" + propertyType + "] and java.sql.Type [" + sqlType + "]";
    throw new RuntimeException(msg);
  }

  public ScalarDataReader<?> getScalarDataReader(Class<?> type) {
    ScalarDataReader<?> reader = typeMap.get(type);
    if (reader == null) {
      reader = compoundTypeMap.get(type);
    }
    return reader;
  }

  @Override
  public ScalarType<?> getJsonScalarType(Class<?> type, int dbType) {

    if (type.equals(Map.class)) {
      // @DbJson Map<String,Object> property
      switch (dbType) {
        case Types.VARCHAR : return jsonMapVarchar;
        case Types.BLOB: return jsonMapBlob;
        case Types.CLOB : return jsonMapClob;
        case DbType.JSONB: return jsonMapJsonb;
        case DbType.JSON: return jsonMapJson;
        default:
          return jsonMapJson;
      }
    }

    if (type.equals(JsonNode.class)) {
      // @DbJson JsonNode property (Jackson)
      switch (dbType) {
        case Types.VARCHAR : return jsonNodeVarchar;
        case Types.BLOB: return jsonNodeBlob;
        case Types.CLOB : return jsonNodeClob;
        case DbType.JSONB: return jsonNodeJsonb;
        case DbType.JSON: return jsonNodeJson;
        default:
          return jsonNodeJson;
      }
    }

    throw new IllegalArgumentException("Type [" + type + "] unsupported for @DbJson mapping");
  }

  /**
   * Return a ScalarType for a given class.
   * <p>
   * Used for java.util.Date and java.util.Calendar which can be mapped to
   * different jdbcTypes in a single system.
   * </p>
   */
  @SuppressWarnings("unchecked")
  public <T> ScalarType<T> getScalarType(Class<T> type, int jdbcType) {

    // File is a special Lob so check for that first
    if (File.class.equals(type)) {
      return (ScalarType<T>) fileType;
    }

    // check for Clob, LongVarchar etc ...
    // the reason being that String maps to multiple jdbc types
    // varchar, clob, longVarchar.
    ScalarType<?> scalarType = getLobTypes(jdbcType);
    if (scalarType != null) {
      // it is a specific Lob type...
      return (ScalarType<T>) scalarType;
    }

    scalarType = typeMap.get(type);
    if (scalarType != null) {
      if (jdbcType == 0 || scalarType.getJdbcType() == jdbcType) {
        // matching type
        return (ScalarType<T>) scalarType;
      }
    }
    // a util Date with jdbcType not matching server wide settings
    if (type.equals(java.util.Date.class)) {
      return (ScalarType<T>) extraTypeFactory.createUtilDate(jsonDateTime, jdbcType);
    }
    // a Calendar with jdbcType not matching server wide settings
    if (type.equals(java.util.Calendar.class)) {
      return (ScalarType<T>) extraTypeFactory.createCalendar(jsonDateTime, jdbcType);
    }

    throw new IllegalArgumentException("Unmatched ScalarType for " + type + " jdbcType:" + jdbcType);
  }

  /**
   * Return the types for the known lob types.
   * <p>
   * Kind of special case because these map multiple jdbc types to single Java
   * types - like String - Varchar, LongVarchar, Clob. For this reason I check
   * for the specific Lob types first before looking for a matching type.
   * </p>
   */
  private ScalarType<?> getLobTypes(int jdbcType) {

    return getScalarType(jdbcType);
  }

  /**
   * Convert the Object to the required datatype. The
   * 
   * @param value
   *          the Object value
   * @param toJdbcType
   *          the type as per java.sql.Types.
   */
  public Object convert(Object value, int toJdbcType) {
    if (value == null) {
      return null;
    }
    ScalarType<?> type = nativeMap.get(toJdbcType);
    if (type != null) {
      return type.toJdbcType(value);
    }
    return value;
  }

  protected boolean isIntegerType(String s) {
    if (isLeadingZeros(s)) {
      return false;
    }
    try {
      Integer.parseInt(s);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Treat leading zeros as a non-integer for enum values.
   */
  private boolean isLeadingZeros(String s) {
    return s.length() > 1 && s.charAt(0) == '0';
  }

  /**
   * Create the Mapping of Enum fields to DB values using EnumValue annotations.
   * <p>
   * Return null if the EnumValue annotations are not present/used.
   * </p>
   */
  private ScalarType<?> createEnumScalarType2(Class<?> enumType) {

    boolean integerType = true;

    Map<String, String> nameValueMap = new HashMap<String, String>();

    Field[] fields = enumType.getDeclaredFields();
    for (int i = 0; i < fields.length; i++) {
      EnumValue enumValue = fields[i].getAnnotation(EnumValue.class);
      if (enumValue != null) {
        nameValueMap.put(fields[i].getName(), enumValue.value());
        if (integerType && !isIntegerType(enumValue.value())) {
          // will treat the values as strings
          integerType = false;
        }
      }
    }
    if (nameValueMap.isEmpty()) {
      // Not using EnumValue here
      return null;
    }

    return createEnumScalarType(enumType, nameValueMap, integerType, 0);
  }
    
  /**
   * Create a ScalarType for an Enum that has additional mapping.
   * <p>
   * The reason for this is that often in a DB there will be short codes used
   * such as A,I,N rather than the ACTIVE, INACTIVE, NEW. So there really needs
   * to be a mapping from the nicely named enumeration values to the typically
   * much shorter codes used in the DB.
   * </p>
   */
  public ScalarType<?> createEnumScalarType(Class<? extends Enum<?>> enumType) {

    Method[] methods = enumType.getMethods();
    for (int i = 0; i <methods.length; i++) {
      DbEnumValue dbValue = methods[i].getAnnotation(DbEnumValue.class);
      if (dbValue != null) {
        boolean integerValues = DbEnumType.INTEGER == dbValue.storage();
        return createEnumScalarTypeDbValue(enumType, methods[i], integerValues);
      }
    }

    // get the mapping information from EnumMapping
    EnumMapping enumMapping = enumType.getAnnotation(EnumMapping.class);
    if (enumMapping == null) {
      // look for EnumValue annotations instead
      return createEnumScalarType2(enumType);
    }

    String nameValuePairs = enumMapping.nameValuePairs();
    boolean integerType = enumMapping.integerType();
    int dbColumnLength = enumMapping.length();

    Map<String, String> nameValueMap = StringHelper.delimitedToMap(nameValuePairs, ",", "=");

    return createEnumScalarType(enumType, nameValueMap, integerType, dbColumnLength);
  }

  /**
   * Create the Mapping of Enum fields to DB values using EnumValue annotations.
   * <p>
   * Return null if the EnumValue annotations are not present/used.
   * </p>
   */
  private ScalarType<?> createEnumScalarTypeDbValue(Class<? extends Enum<?>> enumType, Method method, boolean integerType) {

    Map<String, String> nameValueMap = new HashMap<String, String>();

    Enum<?>[] enumConstants = enumType.getEnumConstants();
    for (int i = 0; i < enumConstants.length; i++) {
      try {
        Object value = method.invoke(enumConstants[i]);
        nameValueMap.put(enumConstants[i].name(), value.toString());
      } catch (Exception e) {
        throw new IllegalArgumentException("Error trying to invoke DbEnumValue method on "+enumConstants[i], e);
      }
    }
    if (nameValueMap.isEmpty()) {
      // Not using EnumValue here
      return null;
    }

    return createEnumScalarType(enumType, nameValueMap, integerType, 0);
  }

  /**
   * Given the name value mapping and integer/string type and explicit DB column
   * length create the ScalarType for the Enum.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private ScalarType<?> createEnumScalarType(Class enumType, Map<String, String> nameValueMap, boolean integerType, int dbColumnLength) {

    EnumToDbValueMap<?> beanDbMap = EnumToDbValueMap.create(integerType);

    int maxValueLen = 0;

    for (Map.Entry<String,String> entry : nameValueMap.entrySet()) {
      String name = entry.getKey();
      String value = entry.getValue();

      maxValueLen = Math.max(maxValueLen, value.length());

      Object enumValue = Enum.valueOf(enumType, name.trim());
      beanDbMap.add(enumValue, value, name.trim());
    }

    if (dbColumnLength == 0 && !integerType) {
      dbColumnLength = maxValueLen;
    }

    return new ScalarTypeEnumWithMapping(beanDbMap, enumType, dbColumnLength);
  }

  /**
   * Automatically find any ScalarTypes by searching through the class path.
   * <p>
   * In avaje.properties define a list of packages in which ScalarTypes are
   * found. This will search for any class that implements the ScalarType
   * interface and register it with this TypeManager.
   * </p>
   */
  protected void initialiseCustomScalarTypes(JsonConfig.DateTime mode, BootupClasses bootupClasses, ServerConfig serverConfig) {

    ScalarTypeLongToTimestamp longToTimestamp = new ScalarTypeLongToTimestamp(mode);

    customScalarTypes.add(longToTimestamp);

    List<Class<?>> foundTypes = bootupClasses.getScalarTypes();

    for (int i = 0; i < foundTypes.size(); i++) {
      Class<?> cls = foundTypes.get(i);
      try {

        ScalarType<?> scalarType;
        if (!objectMapperPresent) {
          scalarType = (ScalarType<?>) cls.newInstance();

        } else {
          try {
            // first try objectMapper constructor
            Constructor<?> constructor = cls.getConstructor(ObjectMapper.class);
            ObjectMapper objectMapper = getObjectMapper(serverConfig);
            scalarType = (ScalarType<?>)constructor.newInstance(objectMapper);
          } catch (NoSuchMethodException e) {
            scalarType = (ScalarType<?>) cls.newInstance();
          }
        }

        add(scalarType);
        customScalarTypes.add(scalarType);

      } catch (Exception e) {
        String msg = "Error loading ScalarType [" + cls.getName() + "]";
        logger.error(msg, e);
      }
    }
  }

  private ObjectMapper getObjectMapper(ServerConfig serverConfig) {

    ObjectMapper objectMapper = (ObjectMapper)serverConfig.getObjectMapper();
    if (objectMapper == null) {
      objectMapper = new ObjectMapper();
      serverConfig.setObjectMapper(objectMapper);
    }
    return objectMapper;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void initialiseScalarConverters(BootupClasses bootupClasses) {

    List<Class<?>> foundTypes = bootupClasses.getScalarConverters();

    for (int i = 0; i < foundTypes.size(); i++) {
      Class<?> cls = foundTypes.get(i);
      try {

        Class<?>[] paramTypes = TypeReflectHelper.getParams(cls, ScalarTypeConverter.class);
        if (paramTypes.length != 2) {
          throw new IllegalStateException("Expected 2 generics paramtypes but got: " + Arrays.toString(paramTypes));
        }

        Class<?> logicalType = paramTypes[0];
        Class<?> persistType = paramTypes[1];

        ScalarType<?> wrappedType = getScalarType(persistType);
        if (wrappedType == null) {
          throw new IllegalStateException("Could not find ScalarType for: " + paramTypes[1]);
        }

        ScalarTypeConverter converter = (ScalarTypeConverter) cls.newInstance();
        ScalarTypeWrapper stw = new ScalarTypeWrapper(logicalType, wrappedType, converter);

        logger.debug("Register ScalarTypeWrapper from " + logicalType + " -> " + persistType + " using:" + cls);

        add(stw);

      } catch (Exception e) {
        String msg = "Error loading ScalarType [" + cls.getName() + "]";
        logger.error(msg, e);
      }
    }

  }

  protected void initialiseCompoundTypes(BootupClasses bootupClasses) {

    List<Class<?>> compoundTypes = bootupClasses.getCompoundTypes();
    for (int j = 0; j < compoundTypes.size(); j++) {

      Class<?> type = compoundTypes.get(j);
      try {

        Class<?>[] paramTypes = TypeReflectHelper.getParams(type, CompoundType.class);
        if (paramTypes.length != 1) {
          throw new RuntimeException("Expecting 1 generic paramter type but got " + Arrays.toString(paramTypes) + " for " + type);
        }

        Class<?> compoundTypeClass = paramTypes[0];

        CompoundType<?> compoundType = (CompoundType<?>) type.newInstance();
        createCompoundScalarDataReader(compoundTypeClass, compoundType, "");

      } catch (Exception e) {
        String msg = "Error initialising component " + type;
        throw new RuntimeException(msg, e);
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected CtCompoundType createCompoundScalarDataReader(Class<?> compoundTypeClass, CompoundType<?> compoundType, String info) {

    CtCompoundType<?> ctCompoundType = compoundTypeMap.get(compoundTypeClass);
    if (ctCompoundType != null) {
      logger.info("Already registered compound type " + compoundTypeClass);
      return ctCompoundType;
    }

    CompoundTypeProperty<?, ?>[] cprops = compoundType.getProperties();

    ScalarDataReader[] dataReaders = new ScalarDataReader[cprops.length];

    for (int i = 0; i < cprops.length; i++) {

      Class<?> propertyType = getCompoundPropertyType(cprops[i]);

      ScalarDataReader<?> scalarDataReader = getScalarDataReader(propertyType, cprops[i].getDbType());
      if (scalarDataReader == null) {
        throw new RuntimeException("Could not find ScalarDataReader for " + propertyType);
      }

      dataReaders[i] = scalarDataReader;
    }

    CtCompoundType ctType = new CtCompoundType(compoundTypeClass, compoundType, dataReaders);

    logger.debug("Registering CompoundType " + compoundTypeClass + " " + info);
    compoundTypeMap.put(compoundTypeClass, ctType);

    return ctType;
  }

  /**
   * Return the property type for a given property of a compound type.
   */
  private Class<?> getCompoundPropertyType(CompoundTypeProperty<?, ?> prop) {

    if (prop instanceof ReflectionBasedCompoundTypeProperty) {
      return ((ReflectionBasedCompoundTypeProperty) prop).getPropertyType();
    }

    // determine the types from generic parameter types using reflection
    Class<?>[] propParamTypes = TypeReflectHelper.getParams(prop.getClass(), CompoundTypeProperty.class);
    if (propParamTypes.length != 2) {
      throw new RuntimeException("Expecting 2 generic paramter types but got " + Arrays.toString(propParamTypes) + " for "
          + prop.getClass());
    }

    return propParamTypes[1];
  }


  /**
   * Add support for Jackson's JsonNode mapping to Clob, Blob, Varchar, JSON and JSONB.
   */
  protected void initialiseJacksonTypes(ServerConfig config) {

    if (config.getClassLoadConfig().isJacksonObjectMapperPresent()) {

      logger.trace("Registering JsonNode type support");

      ObjectMapper objectMapper = (ObjectMapper)config.getObjectMapper();
      if (objectMapper == null) {
        objectMapper = new ObjectMapper();
        config.setObjectMapper(objectMapper);
      }

      jsonNodeClob = new ScalarTypeJsonNode.Clob(objectMapper);
      jsonNodeBlob = new ScalarTypeJsonNode.Blob(objectMapper);
      jsonNodeVarchar = new ScalarTypeJsonNode.Varchar(objectMapper);
      jsonNodeJson = jsonNodeClob;  // Default for non-Postgres databases
      jsonNodeJsonb = jsonNodeClob; // Default for non-Postgres databases

      if (isPostgres(config.getDatabasePlatform())) {
        jsonNodeJson = new ScalarTypeJsonNodePostgres.JSON(objectMapper);
        jsonNodeJsonb = new ScalarTypeJsonNodePostgres.JSONB(objectMapper);
      }

      // add as default mapping for JsonNode (when not annotated with @DbJson etc)
      typeMap.put(JsonNode.class, jsonNodeJson);
    }
  }

  protected void initialiseJavaTimeTypes(JsonConfig.DateTime mode, ServerConfig config) {
    if (config.getClassLoadConfig().isJavaTimePresent()) {
      logger.debug("Registering java.time data types");
      typeMap.put(java.time.LocalDate.class, new ScalarTypeLocalDate());
      typeMap.put(java.time.LocalDateTime.class, new ScalarTypeLocalDateTime(mode));
      typeMap.put(OffsetDateTime.class, new ScalarTypeOffsetDateTime(mode));
      typeMap.put(ZonedDateTime.class, new ScalarTypeZonedDateTime(mode));
      typeMap.put(Instant.class, new ScalarTypeInstant(mode));

      typeMap.put(DayOfWeek.class, new ScalarTypeDayOfWeek());
      typeMap.put(Month.class, new ScalarTypeMonth());
      typeMap.put(Year.class, new ScalarTypeYear());
      typeMap.put(YearMonth.class, new ScalarTypeYearMonthDate());
      typeMap.put(MonthDay.class, new ScalarTypeMonthDay());
      typeMap.put(OffsetTime.class, new ScalarTypeOffsetTime());
      typeMap.put(ZoneId.class, new ScalarTypeZoneId());
      typeMap.put(ZoneOffset.class, new ScalarTypeZoneOffset());

      createCompoundScalarDataReader(Period.class, new CompoundTypePeriod(), "");

      boolean localTimeNanos = config.isLocalTimeWithNanos();
      typeMap.put(java.time.LocalTime.class, (localTimeNanos)? new ScalarTypeLocalTimeWithNanos() : new ScalarTypeLocalTime());

      boolean durationNanos = config.isDurationWithNanos();
      typeMap.put(Duration.class, (durationNanos)? new ScalarTypeDurationWithNanos() : new ScalarTypeDuration());
    }
  }

  /**
   * Detect if Joda classes are in the classpath and if so register the Joda
   * data types.
   */
  protected void initialiseJodaTypes(JsonConfig.DateTime mode, ServerConfig config) {

    // detect if Joda classes are in the classpath
    if (config.getClassLoadConfig().isJodaTimePresent()) {
      // Joda classes are in the classpath so register the types
      logger.debug("Registering Joda data types");
      typeMap.put(LocalDateTime.class, new ScalarTypeJodaLocalDateTime(mode));
      typeMap.put(DateTime.class, new ScalarTypeJodaDateTime(mode));
      typeMap.put(LocalDate.class, new ScalarTypeJodaLocalDate());
      typeMap.put(DateMidnight.class, new ScalarTypeJodaDateMidnight());

      String jodaLocalTimeMode = config.getJodaLocalTimeMode();
      if ("normal".equalsIgnoreCase(jodaLocalTimeMode)) {
        // use the expected/normal local time zone
        typeMap.put(LocalTime.class, new ScalarTypeJodaLocalTime());
        logger.debug("registered ScalarTypeJodaLocalTime");
      } else if ("utc".equalsIgnoreCase(jodaLocalTimeMode)) {
        // use the old UTC based
        typeMap.put(LocalTime.class, new ScalarTypeJodaLocalTimeUTC());
        logger.debug("registered ScalarTypeJodaLocalTimeUTC");
      }
    }
  }
	
  /**
   * Register all the standard types supported. This is the standard JDBC types
   * plus some other common types such as java.util.Date and java.util.Calendar.
   */
  protected void initialiseStandard(JsonConfig.DateTime mode, ServerConfig config) {

    DatabasePlatform databasePlatform = config.getDatabasePlatform();
    int platformClobType = databasePlatform.getClobDbType();
    int platformBlobType = databasePlatform.getBlobDbType();

    nativeMap.put(DbType.HSTORE, new ScalarTypePostgresHstore());

    ScalarType<?> utilDateType = extraTypeFactory.createUtilDate(mode);
    typeMap.put(java.util.Date.class, utilDateType);

    ScalarType<?> calType = extraTypeFactory.createCalendar(mode);
    typeMap.put(Calendar.class, calType);

    ScalarType<?> mathBigIntType = extraTypeFactory.createMathBigInteger();
    typeMap.put(BigInteger.class, mathBigIntType);

    ScalarType<?> booleanType = extraTypeFactory.createBoolean();
    typeMap.put(Boolean.class, booleanType);
    typeMap.put(boolean.class, booleanType);

    // always register Types.BOOLEAN to our boolean type
    nativeMap.put(Types.BOOLEAN, booleanType);
    if (booleanType.getJdbcType() == Types.BIT) {
      // for MapBeans ... BIT types are assumed to be booleans
      nativeMap.put(Types.BIT, booleanType);
    }

    boolean nativeUuidType = databasePlatform.isNativeUuidType();
    ServerConfig.DbUuid dbUuid = config.getDbUuid();

    if (nativeUuidType && dbUuid == ServerConfig.DbUuid.AUTO) {
      // DB has native support for UUID
      typeMap.put(UUID.class, new ScalarTypeUUIDNative());
    } else {
      // Store UUID as binary(16) or varchar(40)
      ScalarType<?> uuidType = (ServerConfig.DbUuid.BINARY == dbUuid) ? new ScalarTypeUUIDBinary() : new ScalarTypeUUIDVarchar();
      typeMap.put(UUID.class, uuidType);
    }

    typeMap.put(File.class, fileType);
    typeMap.put(InetAddress.class, inetAddressType);
    typeMap.put(Locale.class, localeType);
    typeMap.put(Currency.class, currencyType);
    typeMap.put(TimeZone.class, timeZoneType);
    typeMap.put(URL.class, urlType);
    typeMap.put(URI.class, uriType);

    // String types
    typeMap.put(char[].class, charArrayType);
    typeMap.put(char.class, charType);
    typeMap.put(String.class, stringType);
    nativeMap.put(Types.VARCHAR, stringType);
    nativeMap.put(Types.CHAR, stringType);
    nativeMap.put(Types.LONGVARCHAR, longVarcharType);

    // Class<?>
    typeMap.put(Class.class, classType);

    if (platformClobType == Types.CLOB) {
      nativeMap.put(Types.CLOB, clobType);
    } else {
      // for Postgres Clobs handled by Varchar ScalarType...
      ScalarType<?> platClobScalarType = nativeMap.get(Integer.valueOf(platformClobType));
      if (platClobScalarType == null) {
        throw new IllegalArgumentException("Type for dbPlatform clobType [" + clobType + "] not found.");
      }
      nativeMap.put(Types.CLOB, platClobScalarType);
    }

    // Binary type
    typeMap.put(byte[].class, varbinaryType);
    nativeMap.put(Types.BINARY, binaryType);
    nativeMap.put(Types.VARBINARY, varbinaryType);
    nativeMap.put(Types.LONGVARBINARY, longVarbinaryType);

    if (platformBlobType == Types.BLOB) {
      nativeMap.put(Types.BLOB, blobType);
    } else {
      // for Postgres Blobs handled by LongVarbinary ScalarType...
      ScalarType<?> platBlobScalarType = nativeMap.get(Integer.valueOf(platformBlobType));
      if (platBlobScalarType == null) {
        throw new IllegalArgumentException("Type for dbPlatform blobType [" + blobType + "] not found.");
      }
      nativeMap.put(Types.BLOB, platBlobScalarType);
    }

    // Number types
    typeMap.put(Byte.class, byteType);
    typeMap.put(byte.class, byteType);
    nativeMap.put(Types.TINYINT, byteType);

    typeMap.put(Short.class, shortType);
    typeMap.put(short.class, shortType);
    nativeMap.put(Types.SMALLINT, shortType);

    typeMap.put(Integer.class, integerType);
    typeMap.put(int.class, integerType);
    nativeMap.put(Types.INTEGER, integerType);

    typeMap.put(Long.class, longType);
    typeMap.put(long.class, longType);
    nativeMap.put(Types.BIGINT, longType);

    typeMap.put(Double.class, doubleType);
    typeMap.put(double.class, doubleType);
    nativeMap.put(Types.FLOAT, doubleType);// no this is not a bug
    nativeMap.put(Types.DOUBLE, doubleType);

    typeMap.put(Float.class, floatType);
    typeMap.put(float.class, floatType);
    nativeMap.put(Types.REAL, floatType);// no this is not a bug

    typeMap.put(BigDecimal.class, bigDecimalType);
    nativeMap.put(Types.DECIMAL, bigDecimalType);
    nativeMap.put(Types.NUMERIC, bigDecimalType);

    // Temporal types
    typeMap.put(Time.class, timeType);
    nativeMap.put(Types.TIME, timeType);
    typeMap.put(Date.class, dateType);
    nativeMap.put(Types.DATE, dateType);

    ScalarType<?> timestampType = new ScalarTypeTimestamp(mode);

    typeMap.put(Timestamp.class, timestampType);
    nativeMap.put(Types.TIMESTAMP, timestampType);

  }

}
