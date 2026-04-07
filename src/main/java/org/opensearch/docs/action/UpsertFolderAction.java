/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.action;

import org.opensearch.action.ActionType;

public class UpsertFolderAction extends ActionType<UpsertFolderResponse> {
  public static final String NAME = "docs:folder/upsert";
  public static final UpsertFolderAction INSTANCE = new UpsertFolderAction();

  private UpsertFolderAction() {
    super(NAME, UpsertFolderResponse::new);
  }
}
