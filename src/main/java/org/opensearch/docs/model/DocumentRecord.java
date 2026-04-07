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

public class DocumentRecord implements Writeable, ToXContentObject {
  private final String id;
  private final String resourceType;
  private final List<String> allSharedPrincipals;
  private final String title;
  private final String content;
  private final String folder;
  private final String owner;
  private final String lastUpdatedBy;
  private final long createdAt;
  private final long updatedAt;
  private final boolean deleted;
  private final long deletedAt;
  private final String deletedBy;
  private final long seqNo;
  private final long primaryTerm;

  public DocumentRecord(
      String id,
      String resourceType,
      List<String> allSharedPrincipals,
      String title,
      String content,
      String folder,
      String owner,
      String lastUpdatedBy,
      long createdAt,
      long updatedAt,
      boolean deleted,
      long deletedAt,
      String deletedBy,
      long seqNo,
      long primaryTerm) {
    this.id = id;
    this.resourceType = resourceType;
    this.allSharedPrincipals = List.copyOf(allSharedPrincipals);
    this.title = title;
    this.content = content;
    this.folder = folder == null ? "" : folder;
    this.owner = owner;
    this.lastUpdatedBy = lastUpdatedBy;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deleted = deleted;
    this.deletedAt = deletedAt;
    this.deletedBy = deletedBy;
    this.seqNo = seqNo;
    this.primaryTerm = primaryTerm;
  }

  public DocumentRecord(StreamInput in) throws IOException {
    this(
        in.readString(),
        in.readString(),
        in.readStringList(),
        in.readString(),
        in.readString(),
        in.readString(),
        in.readString(),
        in.readString(),
        in.readLong(),
        in.readLong(),
        in.readBoolean(),
        in.readLong(),
        in.readString(),
        in.readLong(),
        in.readLong());
  }

  public String getId() {
    return id;
  }

  public String getResourceType() {
    return resourceType;
  }

  public List<String> getAllSharedPrincipals() {
    return allSharedPrincipals;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public String getFolder() {
    return folder;
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

  public boolean isDeleted() {
    return deleted;
  }

  public long getDeletedAt() {
    return deletedAt;
  }

  public String getDeletedBy() {
    return deletedBy;
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
    return new DocumentSummary(
        id, title, folder, excerpt, lastUpdatedBy, updatedAt, seqNo, primaryTerm);
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    out.writeString(id);
    out.writeString(resourceType);
    out.writeStringCollection(allSharedPrincipals);
    out.writeString(title);
    out.writeString(content);
    out.writeString(folder);
    out.writeString(owner);
    out.writeString(lastUpdatedBy);
    out.writeLong(createdAt);
    out.writeLong(updatedAt);
    out.writeBoolean(deleted);
    out.writeLong(deletedAt);
    out.writeString(deletedBy);
    out.writeLong(seqNo);
    out.writeLong(primaryTerm);
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field("id", id);
    builder.field("resourceType", resourceType);
    builder.field("allSharedPrincipals", allSharedPrincipals);
    builder.field("title", title);
    builder.field("content", content);
    builder.field("folder", folder);
    builder.field("owner", owner);
    builder.field("lastUpdatedBy", lastUpdatedBy);
    builder.field("createdAt", createdAt);
    builder.field("updatedAt", updatedAt);
    builder.field("isDeleted", deleted);
    builder.field("deletedAt", deletedAt);
    builder.field("deletedBy", deletedBy);
    builder.field("seqNo", seqNo);
    builder.field("primaryTerm", primaryTerm);
    builder.endObject();
    return builder;
  }

  public static DocumentRecord fromSource(
      String id, long seqNo, long primaryTerm, Map<String, Object> source) {
    return new DocumentRecord(
        id,
        valueAsString(source.getOrDefault("resource_type", Constants.DOC_RESOURCE_TYPE)),
        valueAsStringList(source.get("all_shared_principals")),
        valueAsString(source.get("title")),
        valueAsString(source.get("content")),
        valueAsString(source.get("folder")),
        valueAsString(source.get("owner")),
        valueAsString(source.get("last_updated_by")),
        valueAsLong(source.get("created_at")),
        valueAsLong(source.get("updated_at")),
        valueAsBoolean(source.get("is_deleted")),
        valueAsLong(source.get("deleted_at")),
        valueAsString(source.get("deleted_by")),
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
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return value == null ? 0L : Long.parseLong(value.toString());
  }

  private static boolean valueAsBoolean(Object value) {
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return value != null && Boolean.parseBoolean(value.toString());
  }
}
