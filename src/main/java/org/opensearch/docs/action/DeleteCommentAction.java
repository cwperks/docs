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

public class DeleteCommentAction extends ActionType<DeleteCommentResponse> {
  public static final String NAME = "docs:comment/delete";
  public static final DeleteCommentAction INSTANCE = new DeleteCommentAction();

  private DeleteCommentAction() {
    super(NAME, DeleteCommentResponse::new);
  }
}
