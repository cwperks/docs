/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.docs.action;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.docs.service.DocumentIndexService;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

public class ListDocumentsTransportAction extends HandledTransportAction<ListDocumentsRequest, ListDocumentsResponse> {
    private final DocumentIndexService documentIndexService;

    @Inject
    public ListDocumentsTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        DocumentIndexService documentIndexService
    ) {
        super(ListDocumentsAction.NAME, transportService, actionFilters, ListDocumentsRequest::new);
        this.documentIndexService = documentIndexService;
    }

    @Override
    protected void doExecute(Task task, ListDocumentsRequest request, ActionListener<ListDocumentsResponse> listener) {
        documentIndexService.listDocuments(request.getQuery(), request.getSize(), listener);
    }
}
