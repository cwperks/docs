/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.action;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.docs.service.DocumentIndexService;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

public class GetDocumentTransportAction
    extends HandledTransportAction<GetDocumentRequest, GetDocumentResponse> {
  private final DocumentIndexService documentIndexService;

  @Inject
  public GetDocumentTransportAction(
      TransportService transportService,
      ActionFilters actionFilters,
      DocumentIndexService documentIndexService) {
    super(GetDocumentAction.NAME, transportService, actionFilters, GetDocumentRequest::new);
    this.documentIndexService = documentIndexService;
  }

  @Override
  protected void doExecute(
      Task task, GetDocumentRequest request, ActionListener<GetDocumentResponse> listener) {
    documentIndexService.getDocument(request.getDocumentId(), listener);
  }
}
