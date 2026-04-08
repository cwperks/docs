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

public class ListCommentsTransportAction
    extends HandledTransportAction<ListCommentsRequest, ListCommentsResponse> {
  private final DocumentIndexService documentIndexService;

  @Inject
  public ListCommentsTransportAction(
      TransportService transportService,
      ActionFilters actionFilters,
      DocumentIndexService documentIndexService) {
    super(ListCommentsAction.NAME, transportService, actionFilters, ListCommentsRequest::new);
    this.documentIndexService = documentIndexService;
  }

  @Override
  protected void doExecute(
      Task task, ListCommentsRequest request, ActionListener<ListCommentsResponse> listener) {
    documentIndexService.listComments(request.getDocumentId(), listener);
  }
}
