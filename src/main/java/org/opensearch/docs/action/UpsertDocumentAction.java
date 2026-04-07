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

public class UpsertDocumentAction extends ActionType<UpsertDocumentResponse> {
  public static final String NAME = "docs:document/upsert";
  public static final UpsertDocumentAction INSTANCE = new UpsertDocumentAction();

  private UpsertDocumentAction() {
    super(NAME, UpsertDocumentResponse::new);
  }
}
