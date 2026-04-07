/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.docs.action;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.Strings;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

import static org.opensearch.action.ValidateActions.addValidationError;

public class GetDocumentRequest extends ActionRequest {
    private final String documentId;

    public GetDocumentRequest(String documentId) {
        this.documentId = documentId;
    }

    public GetDocumentRequest(StreamInput in) throws IOException {
        super(in);
        this.documentId = in.readString();
    }

    public String getDocumentId() {
        return documentId;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (Strings.hasText(documentId) == false) {
            validationException = addValidationError("documentId is required", validationException);
        }
        return validationException;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(documentId);
    }
}
