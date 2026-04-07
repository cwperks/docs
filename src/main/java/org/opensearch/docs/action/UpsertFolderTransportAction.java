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

public class UpsertFolderTransportAction
    extends HandledTransportAction<UpsertFolderRequest, UpsertFolderResponse> {
  private final DocumentIndexService documentIndexService;
  private final ThreadPool threadPool;

  @Inject
  public UpsertFolderTransportAction(
      TransportService transportService,
      ActionFilters actionFilters,
      DocumentIndexService documentIndexService) {
    super(UpsertFolderAction.NAME, transportService, actionFilters, UpsertFolderRequest::new);
    this.documentIndexService = documentIndexService;
    this.threadPool = transportService.getThreadPool();
  }

  @Override
  protected void doExecute(
      Task task, UpsertFolderRequest request, ActionListener<UpsertFolderResponse> listener) {
    String actor = "unknown";
    String userString =
        threadPool
            .getThreadContext()
            .getTransient(ConfigConstants.OPENSEARCH_SECURITY_USER_INFO_THREAD_CONTEXT);
    if (userString != null) {
      actor = User.parse(userString).getName();
    }
    documentIndexService.upsertFolder(request, actor, listener);
  }
}
