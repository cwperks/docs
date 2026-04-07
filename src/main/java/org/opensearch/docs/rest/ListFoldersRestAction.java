/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.rest;

import static org.opensearch.rest.RestRequest.Method.GET;

import java.io.IOException;
import java.util.List;
import org.opensearch.docs.Constants;
import org.opensearch.docs.action.ListFoldersAction;
import org.opensearch.docs.action.ListFoldersRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

public class ListFoldersRestAction extends BaseRestHandler {
  @Override
  public List<Route> routes() {
    return List.of(new Route(GET, Constants.FOLDERS_API_PATH));
  }

  @Override
  public String getName() {
    return "docs_list_folders";
  }

  @Override
  protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client)
      throws IOException {
    String query = request.param("query");
    int size = request.paramAsInt("size", 200);
    ListFoldersRequest actionRequest = new ListFoldersRequest(query, Math.min(size, 500));
    return channel ->
        client.executeLocally(
            ListFoldersAction.INSTANCE, actionRequest, new RestToXContentListener<>(channel));
  }
}
