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
import java.util.List;
import java.util.Map;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.docs.Constants;

public class CommentRecord implements Writeable, ToXContentObject {
  private final String id;
  private final String resourceType;
  private final String documentId;
  private final String threadId;
  private final String commentText;
  private final int startOffset;
  private final int endOffset;
  private final String owner;
  private final long createdAt;
  private final long updatedAt;
  private final boolean deleted;
  private final List<String> readBy;
  private final long seqNo;
  private final long primaryTerm;

  public CommentRecord(
      String id,
      String resourceType,
      String documentId,
      String threadId,
      String commentText,
      int startOffset,
      int endOffset,
      String owner,
      long createdAt,
      long updatedAt,
      boolean deleted,
      List<String> readBy,
      long seqNo,
      long primaryTerm) {
    this.id = id;
    this.resourceType = resourceType;
    this.documentId = documentId;
    this.threadId = threadId;
    this.commentText = commentText;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
    this.owner = owner;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deleted = deleted;
    this.readBy = List.copyOf(readBy);
    this.seqNo = seqNo;
    this.primaryTerm = primaryTerm;
  }

  public CommentRecord(StreamInput in) throws IOException {
    this(
        in.readString(),
        in.readString(),
        in.readString(),
        in.readString(),
        in.readString(),
        in.readInt(),
        in.readInt(),
        in.readString(),
        in.readLong(),
        in.readLong(),
        in.readBoolean(),
        in.readStringList(),
        in.readLong(),
        in.readLong());
  }

  public String getId() {
    return id;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getDocumentId() {
    return documentId;
  }

  public String getThreadId() {
    return threadId;
  }

  public String getCommentText() {
    return commentText;
  }

  public int getStartOffset() {
    return startOffset;
  }

  public int getEndOffset() {
    return endOffset;
  }

  public String getOwner() {
    return owner;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public List<String> getReadBy() {
    return readBy;
  }

  public long getSeqNo() {
    return seqNo;
  }

  public long getPrimaryTerm() {
    return primaryTerm;
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    out.writeString(id);
    out.writeString(resourceType);
    out.writeString(documentId);
    out.writeString(threadId);
    out.writeString(commentText);
    out.writeInt(startOffset);
    out.writeInt(endOffset);
    out.writeString(owner);
    out.writeLong(createdAt);
    out.writeLong(updatedAt);
    out.writeBoolean(deleted);
    out.writeStringCollection(readBy);
    out.writeLong(seqNo);
    out.writeLong(primaryTerm);
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field("id", id);
    builder.field("documentId", documentId);
    builder.field("threadId", threadId);
    builder.field("commentText", commentText);
    builder.field("startOffset", startOffset);
    builder.field("endOffset", endOffset);
    builder.field("owner", owner);
    builder.field("createdAt", createdAt);
    builder.field("updatedAt", updatedAt);
    builder.field("isDeleted", deleted);
    builder.field("readBy", readBy);
    builder.field("seqNo", seqNo);
    builder.field("primaryTerm", primaryTerm);
    builder.endObject();
    return builder;
  }

  public static CommentRecord fromSource(
      String id, long seqNo, long primaryTerm, Map<String, Object> source) {
    return new CommentRecord(
        id,
        valueAsString(source.getOrDefault("resource_type", Constants.COMMENT_RESOURCE_TYPE)),
        valueAsString(source.get("document_id")),
        valueAsString(source.get("thread_id")),
        valueAsString(source.get("comment_text")),
        valueAsInt(source.get("start_offset")),
        valueAsInt(source.get("end_offset")),
        valueAsString(source.get("owner")),
        valueAsLong(source.get("created_at")),
        valueAsLong(source.get("updated_at")),
        valueAsBoolean(source.get("is_deleted")),
        valueAsStringList(source.get("read_by")),
        seqNo,
        primaryTerm);
  }

  private static List<String> valueAsStringList(Object value) {
    if (value instanceof List<?>) {
      return ((List<?>) value).stream().map(String::valueOf).toList();
    }
    return List.of();
  }

  private static String valueAsString(Object value) {
    return value == null ? "" : value.toString();
  }

  private static long valueAsLong(Object value) {
    if (value instanceof Number) return ((Number) value).longValue();
    return value == null ? 0L : Long.parseLong(value.toString());
  }

  private static int valueAsInt(Object value) {
    if (value instanceof Number) return ((Number) value).intValue();
    return value == null ? 0 : Integer.parseInt(value.toString());
  }

  private static boolean valueAsBoolean(Object value) {
    if (value instanceof Boolean) return (Boolean) value;
    return value != null && Boolean.parseBoolean(value.toString());
  }
}
