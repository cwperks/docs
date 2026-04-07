/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.rest;

import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.rest.RestRequest.Method.PUT;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.docs.Constants;
import org.opensearch.docs.action.UpsertFolderAction;
import org.opensearch.docs.action.UpsertFolderRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

public class UpsertFolderRestAction extends BaseRestHandler {
  @Override
  public List<Route> routes() {
    return List.of(
        new Route(PUT, Constants.FOLDERS_API_PATH),
        new Route(POST, Constants.FOLDERS_API_PATH + "/{folder_id}"));
  }

  @Override
  public String getName() {
    return "docs_upsert_folder";
  }

  @Override
  protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client)
      throws IOException {
    Map<String, Object> source;
    try (XContentParser parser = request.contentParser()) {
      source = parser.map();
    }

    String name = source.get("name") == null ? null : source.get("name").toString();
    String parentId = source.get("parentId") == null ? "" : source.get("parentId").toString();
    Long seqNo =
        source.get("seqNo") instanceof Number ? ((Number) source.get("seqNo")).longValue() : null;
    Long primaryTerm =
        source.get("primaryTerm") instanceof Number
            ? ((Number) source.get("primaryTerm")).longValue()
            : null;

    UpsertFolderRequest actionRequest =
        new UpsertFolderRequest(request.param("folder_id"), name, parentId, seqNo, primaryTerm);
    return channel ->
        client.executeLocally(
            UpsertFolderAction.INSTANCE, actionRequest, new RestToXContentListener<>(channel));
  }
}
