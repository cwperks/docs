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

public class GetDocumentAction extends ActionType<GetDocumentResponse> {
  public static final String NAME = "cluster:admin/opensearch/docs/document/get";
  public static final GetDocumentAction INSTANCE = new GetDocumentAction();

  private GetDocumentAction() {
    super(NAME, GetDocumentResponse::new);
  }
}
