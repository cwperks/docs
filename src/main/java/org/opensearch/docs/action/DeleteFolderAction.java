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

public class DeleteFolderAction extends ActionType<DeleteFolderResponse> {
  public static final String NAME = "docs:folder/delete";
  public static final DeleteFolderAction INSTANCE = new DeleteFolderAction();

  private DeleteFolderAction() {
    super(NAME, DeleteFolderResponse::new);
  }
}
