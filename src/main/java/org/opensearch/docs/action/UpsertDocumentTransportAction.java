/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.docs.action;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.commons.ConfigConstants;
import org.opensearch.commons.authuser.User;
import org.opensearch.core.action.ActionListener;
import org.opensearch.docs.service.DocumentIndexService;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

public class UpsertDocumentTransportAction extends HandledTransportAction<UpsertDocumentRequest, UpsertDocumentResponse> {
    private final DocumentIndexService documentIndexService;
    private final ThreadPool threadPool;

    @Inject
    public UpsertDocumentTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        DocumentIndexService documentIndexService
    ) {
        super(UpsertDocumentAction.NAME, transportService, actionFilters, UpsertDocumentRequest::new);
        this.documentIndexService = documentIndexService;
        this.threadPool = transportService.getThreadPool();
    }

    @Override
    protected void doExecute(Task task, UpsertDocumentRequest request, ActionListener<UpsertDocumentResponse> listener) {
        String actor = "unknown";
        String userString = threadPool.getThreadContext().getTransient(ConfigConstants.OPENSEARCH_SECURITY_USER_INFO_THREAD_CONTEXT);
        if (userString != null) {
            actor = User.parse(userString).getName();
        }
        documentIndexService.upsertDocument(request, actor, listener);
    }
}
