package com.avaje.ebean.dbmigration.ddlgeneration.platform;

import com.avaje.ebean.config.dbplatform.DbIdentity;
import com.avaje.ebean.config.dbplatform.DbTypeMap;

/**
 * Hsqldb platform specific DDL.
 */
public class HsqldbDdl extends PlatformDdl {

  public HsqldbDdl(DbTypeMap platformTypes, DbIdentity dbIdentity) {
    super(platformTypes, dbIdentity);
    this.identitySuffix = " generated by default as identity (start with 1) ";
  }

}
