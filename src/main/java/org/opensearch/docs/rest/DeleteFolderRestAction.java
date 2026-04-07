/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.rest;

import static org.opensearch.rest.RestRequest.Method.DELETE;

import java.io.IOException;
import java.util.List;
import org.opensearch.docs.Constants;
import org.opensearch.docs.action.DeleteFolderAction;
import org.opensearch.docs.action.DeleteFolderRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

public class DeleteFolderRestAction extends BaseRestHandler {
  @Override
  public List<Route> routes() {
    return List.of(new Route(DELETE, Constants.FOLDERS_API_PATH + "/{folder_id}"));
  }

  @Override
  public String getName() {
    return "docs_delete_folder";
  }

  @Override
  protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client)
      throws IOException {
    DeleteFolderRequest actionRequest =
        new DeleteFolderRequest(
            request.param("folder_id"),
            request.hasParam("seqNo") ? request.paramAsLong("seqNo", -1L) : null,
            request.hasParam("primaryTerm") ? request.paramAsLong("primaryTerm", -1L) : null);
    return channel ->
        client.executeLocally(
            DeleteFolderAction.INSTANCE, actionRequest, new RestToXContentListener<>(channel));
  }
}
