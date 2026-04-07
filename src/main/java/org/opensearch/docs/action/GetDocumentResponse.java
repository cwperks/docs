/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.docs.action;

import java.io.IOException;

import org.opensearch.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.docs.model.DocumentRecord;

public class GetDocumentResponse extends ActionResponse implements ToXContentObject {
    private final DocumentRecord document;

    public GetDocumentResponse(DocumentRecord document) {
        this.document = document;
    }

    public GetDocumentResponse(StreamInput in) throws IOException {
        this(new DocumentRecord(in));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        document.writeTo(out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("document", document);
        builder.endObject();
        return builder;
    }
}
