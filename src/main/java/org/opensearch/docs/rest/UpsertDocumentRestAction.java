/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.docs.rest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.docs.Constants;
import org.opensearch.docs.action.UpsertDocumentAction;
import org.opensearch.docs.action.UpsertDocumentRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.rest.RestRequest.Method.PUT;

public class UpsertDocumentRestAction extends BaseRestHandler {
    @Override
    public List<Route> routes() {
        return List.of(
            new Route(PUT, Constants.DOCUMENTS_API_PATH),
            new Route(POST, Constants.DOCUMENTS_API_PATH + "/{document_id}")
        );
    }

    @Override
    public String getName() {
        return "docs_upsert_document";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        Map<String, Object> source;
        try (XContentParser parser = request.contentParser()) {
            source = parser.map();
        }

        String title = source.get("title") == null ? null : source.get("title").toString();
        String content = source.get("content") == null ? "" : source.get("content").toString();
        Long seqNo = source.get("seqNo") instanceof Number ? ((Number) source.get("seqNo")).longValue() : null;
        Long primaryTerm = source.get("primaryTerm") instanceof Number ? ((Number) source.get("primaryTerm")).longValue() : null;
        String documentId = request.param("document_id");

        UpsertDocumentRequest actionRequest = new UpsertDocumentRequest(documentId, title, content, seqNo, primaryTerm);
        return channel -> client.executeLocally(UpsertDocumentAction.INSTANCE, actionRequest, new RestToXContentListener<>(channel));
    }
}
