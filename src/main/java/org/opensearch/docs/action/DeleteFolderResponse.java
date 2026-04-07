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

public class DeleteFolderResponse extends ActionResponse implements ToXContentObject {
  private final boolean deleted;
  private final String folderId;

  public DeleteFolderResponse(boolean deleted, String folderId) {
    this.deleted = deleted;
    this.folderId = folderId;
  }

  public DeleteFolderResponse(StreamInput in) throws IOException {
    this(in.readBoolean(), in.readString());
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    out.writeBoolean(deleted);
    out.writeString(folderId);
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field("deleted", deleted);
    builder.field("folderId", folderId);
    builder.endObject();
    return builder;
  }
}
