/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.docs.action;

import org.opensearch.action.ActionType;

public class UpsertDocumentAction extends ActionType<UpsertDocumentResponse> {
    public static final String NAME = "cluster:admin/opensearch/docs/document/upsert";
    public static final UpsertDocumentAction INSTANCE = new UpsertDocumentAction();

    private UpsertDocumentAction() {
        super(NAME, UpsertDocumentResponse::new);
    }
}
