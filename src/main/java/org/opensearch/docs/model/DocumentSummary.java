/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.model;

import java.io.IOException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

public class DocumentSummary implements Writeable, ToXContentObject {
  private final String id;
  private final String title;
  private final String folder;
  private final String excerpt;
  private final String lastUpdatedBy;
  private final long updatedAt;
  private final long seqNo;
  private final long primaryTerm;

  public DocumentSummary(
      String id,
      String title,
      String folder,
      String excerpt,
      String lastUpdatedBy,
      long updatedAt,
      long seqNo,
      long primaryTerm) {
    this.id = id;
    this.title = title;
    this.folder = folder;
    this.excerpt = excerpt;
    this.lastUpdatedBy = lastUpdatedBy;
    this.updatedAt = updatedAt;
    this.seqNo = seqNo;
    this.primaryTerm = primaryTerm;
  }

  public DocumentSummary(StreamInput in) throws IOException {
    this(
        in.readString(),
        in.readString(),
        in.readString(),
        in.readString(),
        in.readString(),
        in.readLong(),
        in.readLong(),
        in.readLong());
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    out.writeString(id);
    out.writeString(title);
    out.writeString(folder);
    out.writeString(excerpt);
    out.writeString(lastUpdatedBy);
    out.writeLong(updatedAt);
    out.writeLong(seqNo);
    out.writeLong(primaryTerm);
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field("id", id);
    builder.field("title", title);
    builder.field("folder", folder);
    builder.field("excerpt", excerpt);
    builder.field("lastUpdatedBy", lastUpdatedBy);
    builder.field("updatedAt", updatedAt);
    builder.field("seqNo", seqNo);
    builder.field("primaryTerm", primaryTerm);
    builder.endObject();
    return builder;
  }
}
