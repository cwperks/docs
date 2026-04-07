/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.docs.rest;

import java.io.IOException;
import java.util.List;

import org.opensearch.docs.Constants;
import org.opensearch.docs.action.GetDocumentAction;
import org.opensearch.docs.action.GetDocumentRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

import static org.opensearch.rest.RestRequest.Method.GET;

public class GetDocumentRestAction extends BaseRestHandler {
    @Override
    public List<Route> routes() {
        return List.of(new Route(GET, Constants.DOCUMENTS_API_PATH + "/{document_id}"));
    }

    @Override
    public String getName() {
        return "docs_get_document";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        GetDocumentRequest actionRequest = new GetDocumentRequest(request.param("document_id"));
        return channel -> client.executeLocally(GetDocumentAction.INSTANCE, actionRequest, new RestToXContentListener<>(channel));
    }
}
