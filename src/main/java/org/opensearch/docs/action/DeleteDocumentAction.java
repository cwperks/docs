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

public class DeleteDocumentAction extends ActionType<DeleteDocumentResponse> {
  public static final String NAME = "docs:document/delete";
  public static final DeleteDocumentAction INSTANCE = new DeleteDocumentAction();

  private DeleteDocumentAction() {
    super(NAME, DeleteDocumentResponse::new);
  }
}
