package com.avaje.ebeanservice.elastic;


import com.squareup.okhttp.*;

import java.io.IOException;

/**
 * Basic implementation for sending the JSON payload to the ElasticSearch Bulk API.
 */
public class BaseHttpMessageSender implements IndexMessageSender {

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient client = new OkHttpClient();

  private final String baseUrl;

  private final String bulkUrl;

  public BaseHttpMessageSender(String baseUrl) {
    this.baseUrl = normaliseBaseUrl(baseUrl);
    this.bulkUrl = deriveBulkUrl(this.baseUrl);
  }

  protected String normaliseBaseUrl(String baseUrl) {
    if (baseUrl == null) return null;
    return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
  }

  /**
   * Return the Bulk API URL given the base URL.
   */
  protected String deriveBulkUrl(String baseUrl) {

    if (baseUrl == null) return null;
    return baseUrl + "_bulk";
  }

  @Override
  public IndexMessageSenderResponse getDocSource(String indexType, String indexName, String docId) throws IOException {

    String url = baseUrl + indexType + "/" + indexName + "/" + docId + "/_source";
    Request request = new Request.Builder()
        .url(url)
        .get().build();

    Response response = client.newCall(request).execute();

    return new IndexMessageSenderResponse(response.code(), response.body().string());
  }

  @Override
  public String postBulk(String json) throws IOException {

    RequestBody body = RequestBody.create(JSON, json);
    Request request = new Request.Builder()
        .url(bulkUrl)
        .put(body)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }
}