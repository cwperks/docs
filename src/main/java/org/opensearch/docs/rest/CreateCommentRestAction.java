/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.rest;

import static org.opensearch.rest.RestRequest.Method.PUT;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.docs.Constants;
import org.opensearch.docs.action.CreateCommentAction;
import org.opensearch.docs.action.CreateCommentRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

public class CreateCommentRestAction extends BaseRestHandler {
  @Override
  public List<Route> routes() {
    return List.of(new Route(PUT, Constants.COMMENTS_API_PATH));
  }

  @Override
  public String getName() {
    return "docs_create_comment";
  }

  @Override
  protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client)
      throws IOException {
    Map<String, Object> source;
    try (XContentParser parser = request.contentParser()) {
      source = parser.map();
    }
    CreateCommentRequest actionRequest =
        new CreateCommentRequest(
            (String) source.get("documentId"),
            (String) source.get("threadId"),
            (String) source.get("commentText"),
            source.containsKey("startOffset") ? ((Number) source.get("startOffset")).intValue() : 0,
            source.containsKey("endOffset") ? ((Number) source.get("endOffset")).intValue() : 0);
    return channel ->
        client.executeLocally(
            CreateCommentAction.INSTANCE, actionRequest, new RestToXContentListener<>(channel));
  }
}
