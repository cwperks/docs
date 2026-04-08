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
import org.opensearch.commons.ConfigConstants;
import org.opensearch.commons.authuser.User;
import org.opensearch.core.action.ActionListener;
import org.opensearch.docs.service.DocumentIndexService;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

public class DeleteCommentTransportAction
    extends HandledTransportAction<DeleteCommentRequest, DeleteCommentResponse> {
  private final DocumentIndexService documentIndexService;
  private final ThreadPool threadPool;

  @Inject
  public DeleteCommentTransportAction(
      TransportService transportService,
      ActionFilters actionFilters,
      DocumentIndexService documentIndexService) {
    super(DeleteCommentAction.NAME, transportService, actionFilters, DeleteCommentRequest::new);
    this.documentIndexService = documentIndexService;
    this.threadPool = transportService.getThreadPool();
  }

  @Override
  protected void doExecute(
      Task task, DeleteCommentRequest request, ActionListener<DeleteCommentResponse> listener) {
    String actor = "unknown";
    String userString =
        threadPool
            .getThreadContext()
            .getTransient(ConfigConstants.OPENSEARCH_SECURITY_USER_INFO_THREAD_CONTEXT);
    if (userString != null) {
      actor = User.parse(userString).getName();
    }
    documentIndexService.deleteComment(request, actor, listener);
  }
}
