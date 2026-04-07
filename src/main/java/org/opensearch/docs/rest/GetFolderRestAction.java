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
import org.opensearch.docs.action.GetFolderAction;
import org.opensearch.docs.action.GetFolderRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

public class GetFolderRestAction extends BaseRestHandler {
  @Override
  public List<Route> routes() {
    return List.of(new Route(GET, Constants.FOLDERS_API_PATH + "/{folder_id}"));
  }

  @Override
  public String getName() {
    return "docs_get_folder";
  }

  @Override
  protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client)
      throws IOException {
    GetFolderRequest actionRequest = new GetFolderRequest(request.param("folder_id"));
    return channel ->
        client.executeLocally(
            GetFolderAction.INSTANCE, actionRequest, new RestToXContentListener<>(channel));
  }
}
