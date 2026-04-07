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

public class FolderSummary implements Writeable, ToXContentObject {
  private final String id;
  private final String name;
  private final String path;
  private final String parentId;
  private final String lastUpdatedBy;
  private final long updatedAt;
  private final long seqNo;
  private final long primaryTerm;

  public FolderSummary(
      String id,
      String name,
      String path,
      String parentId,
      String lastUpdatedBy,
      long updatedAt,
      long seqNo,
      long primaryTerm) {
    this.id = id;
    this.name = name;
    this.path = path;
    this.parentId = parentId;
    this.lastUpdatedBy = lastUpdatedBy;
    this.updatedAt = updatedAt;
    this.seqNo = seqNo;
    this.primaryTerm = primaryTerm;
  }

  public FolderSummary(StreamInput in) throws IOException {
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
    out.writeString(name);
    out.writeString(path);
    out.writeString(parentId);
    out.writeString(lastUpdatedBy);
    out.writeLong(updatedAt);
    out.writeLong(seqNo);
    out.writeLong(primaryTerm);
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field("id", id);
    builder.field("name", name);
    builder.field("path", path);
    builder.field("parentId", parentId);
    builder.field("lastUpdatedBy", lastUpdatedBy);
    builder.field("updatedAt", updatedAt);
    builder.field("seqNo", seqNo);
    builder.field("primaryTerm", primaryTerm);
    builder.endObject();
    return builder;
  }
}
