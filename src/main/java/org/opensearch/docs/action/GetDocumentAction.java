/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
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
