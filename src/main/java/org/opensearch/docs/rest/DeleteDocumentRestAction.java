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
import org.opensearch.docs.action.DeleteDocumentAction;
import org.opensearch.docs.action.DeleteDocumentRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

public class DeleteDocumentRestAction extends BaseRestHandler {
  @Override
  public List<Route> routes() {
    return List.of(new Route(DELETE, Constants.DOCUMENTS_API_PATH + "/{document_id}"));
  }

  @Override
  public String getName() {
    return "docs_delete_document";
  }

  @Override
  protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client)
      throws IOException {
    String documentId = request.param("document_id");
    Long seqNo = request.hasParam("seqNo") ? request.paramAsLong("seqNo", -1L) : null;
    Long primaryTerm =
        request.hasParam("primaryTerm") ? request.paramAsLong("primaryTerm", -1L) : null;

    DeleteDocumentRequest actionRequest =
        new DeleteDocumentRequest(documentId, seqNo, primaryTerm);
    return channel ->
        client.executeLocally(
            DeleteDocumentAction.INSTANCE, actionRequest, new RestToXContentListener<>(channel));
  }
}
