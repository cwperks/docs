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
import java.util.Map;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

public class DocumentRecord implements Writeable, ToXContentObject {
  private final String id;
  private final String title;
  private final String content;
  private final String owner;
  private final String lastUpdatedBy;
  private final long createdAt;
  private final long updatedAt;
  private final long seqNo;
  private final long primaryTerm;

  public DocumentRecord(
      String id,
      String title,
      String content,
      String owner,
      String lastUpdatedBy,
      long createdAt,
      long updatedAt,
      long seqNo,
      long primaryTerm) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.owner = owner;
    this.lastUpdatedBy = lastUpdatedBy;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.seqNo = seqNo;
    this.primaryTerm = primaryTerm;
  }

  public DocumentRecord(StreamInput in) throws IOException {
    this(
        in.readString(),
        in.readString(),
        in.readString(),
        in.readString(),
        in.readString(),
        in.readLong(),
        in.readLong(),
        in.readLong(),
        in.readLong());
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public String getOwner() {
    return owner;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public long getSeqNo() {
    return seqNo;
  }

  public long getPrimaryTerm() {
    return primaryTerm;
  }

  public DocumentSummary toSummary() {
    String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
    String excerpt = normalized.length() > 180 ? normalized.substring(0, 180) + "..." : normalized;
    return new DocumentSummary(id, title, excerpt, lastUpdatedBy, updatedAt, seqNo, primaryTerm);
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    out.writeString(id);
    out.writeString(title);
    out.writeString(content);
    out.writeString(owner);
    out.writeString(lastUpdatedBy);
    out.writeLong(createdAt);
    out.writeLong(updatedAt);
    out.writeLong(seqNo);
    out.writeLong(primaryTerm);
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field("id", id);
    builder.field("title", title);
    builder.field("content", content);
    builder.field("owner", owner);
    builder.field("lastUpdatedBy", lastUpdatedBy);
    builder.field("createdAt", createdAt);
    builder.field("updatedAt", updatedAt);
    builder.field("seqNo", seqNo);
    builder.field("primaryTerm", primaryTerm);
    builder.endObject();
    return builder;
  }

  public static DocumentRecord fromSource(
      String id, long seqNo, long primaryTerm, Map<String, Object> source) {
    return new DocumentRecord(
        id,
        valueAsString(source.get("title")),
        valueAsString(source.get("content")),
        valueAsString(source.get("owner")),
        valueAsString(source.get("last_updated_by")),
        valueAsLong(source.get("created_at")),
        valueAsLong(source.get("updated_at")),
        seqNo,
        primaryTerm);
  }

  private static String valueAsString(Object value) {
    return value == null ? "" : value.toString();
  }

  private static long valueAsLong(Object value) {
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return value == null ? 0L : Long.parseLong(value.toString());
  }
}
