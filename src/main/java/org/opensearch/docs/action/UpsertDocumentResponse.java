/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.action;

import java.io.IOException;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.docs.model.DocumentRecord;

public class UpsertDocumentResponse extends ActionResponse implements ToXContentObject {
  private final boolean created;
  private final DocumentRecord document;

  public UpsertDocumentResponse(boolean created, DocumentRecord document) {
    this.created = created;
    this.document = document;
  }

  public UpsertDocumentResponse(StreamInput in) throws IOException {
    this(in.readBoolean(), new DocumentRecord(in));
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    out.writeBoolean(created);
    document.writeTo(out);
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field("created", created);
    builder.field("document", document);
    builder.endObject();
    return builder;
  }
}
