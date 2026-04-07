/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.docs.action;

import java.io.IOException;
import java.util.List;

import org.opensearch.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.docs.model.DocumentSummary;

public class ListDocumentsResponse extends ActionResponse implements ToXContentObject {
    private final List<DocumentSummary> documents;

    public ListDocumentsResponse(List<DocumentSummary> documents) {
        this.documents = documents;
    }

    public ListDocumentsResponse(StreamInput in) throws IOException {
        this(in.readList(DocumentSummary::new));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeList(documents);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("documents", documents);
        builder.endObject();
        return builder;
    }
}
