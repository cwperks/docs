/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.docs.rest;

import java.io.IOException;
import java.util.List;

import org.opensearch.docs.Constants;
import org.opensearch.docs.action.ListDocumentsAction;
import org.opensearch.docs.action.ListDocumentsRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

import static org.opensearch.rest.RestRequest.Method.GET;

public class ListDocumentsRestAction extends BaseRestHandler {
    @Override
    public List<Route> routes() {
        return List.of(new Route(GET, Constants.DOCUMENTS_API_PATH));
    }

    @Override
    public String getName() {
        return "docs_list_documents";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String query = request.param("query");
        int size = request.paramAsInt("size", 50);
        ListDocumentsRequest actionRequest = new ListDocumentsRequest(query, Math.min(size, 200));
        return channel -> client.executeLocally(ListDocumentsAction.INSTANCE, actionRequest, new RestToXContentListener<>(channel));
    }
}
