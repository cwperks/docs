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
import org.opensearch.docs.action.DeleteCommentAction;
import org.opensearch.docs.action.DeleteCommentRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

public class DeleteCommentRestAction extends BaseRestHandler {
  @Override
  public List<Route> routes() {
    return List.of(new Route(DELETE, Constants.COMMENTS_API_PATH + "/{commentId}"));
  }

  @Override
  public String getName() {
    return "docs_delete_comment";
  }

  @Override
  protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client)
      throws IOException {
    String commentId = request.param("commentId");
    long seqNo = request.paramAsLong("seqNo", -1);
    long primaryTerm = request.paramAsLong("primaryTerm", -1);
    DeleteCommentRequest actionRequest = new DeleteCommentRequest(commentId, seqNo, primaryTerm);
    return channel ->
        client.executeLocally(
            DeleteCommentAction.INSTANCE, actionRequest, new RestToXContentListener<>(channel));
  }
}
