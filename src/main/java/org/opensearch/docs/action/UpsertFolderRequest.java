/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.action;

import static org.opensearch.action.ValidateActions.addValidationError;

import java.io.IOException;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.DocRequest;
import org.opensearch.core.common.Strings;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.docs.Constants;

public class UpsertFolderRequest extends ActionRequest implements DocRequest {
  private final String folderId;
  private final String name;
  private final String parentId;
  private final Long seqNo;
  private final Long primaryTerm;

  public UpsertFolderRequest(
      String folderId, String name, String parentId, Long seqNo, Long primaryTerm) {
    this.folderId = folderId;
    this.name = name;
    this.parentId = parentId == null ? "" : parentId.trim();
    this.seqNo = seqNo;
    this.primaryTerm = primaryTerm;
  }

  public UpsertFolderRequest(StreamInput in) throws IOException {
    super(in);
    this.folderId = in.readOptionalString();
    this.name = in.readString();
    this.parentId = in.readString();
    this.seqNo = in.readOptionalLong();
    this.primaryTerm = in.readOptionalLong();
  }

  public String getFolderId() {
    return folderId;
  }

  public String getName() {
    return name;
  }

  public String getParentId() {
    return parentId;
  }

  public long getSeqNo() {
    return seqNo == null ? -1L : seqNo;
  }

  public long getPrimaryTerm() {
    return primaryTerm == null ? -1L : primaryTerm;
  }

  @Override
  public String index() {
    return Constants.DOCS_INDEX;
  }

  @Override
  public String id() {
    return folderId;
  }

  @Override
  public String type() {
    return Constants.FOLDER_RESOURCE_TYPE;
  }

  @Override
  public ActionRequestValidationException validate() {
    ActionRequestValidationException validationException = null;
    if (Strings.hasText(name) == false) {
      validationException = addValidationError("name is required", validationException);
    }
    if (folderId != null && (seqNo == null || primaryTerm == null)) {
      validationException =
          addValidationError(
              "seqNo and primaryTerm are required when updating an existing folder",
              validationException);
    }
    return validationException;
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    super.writeTo(out);
    out.writeOptionalString(folderId);
    out.writeString(name);
    out.writeString(parentId);
    out.writeOptionalLong(seqNo);
    out.writeOptionalLong(primaryTerm);
  }
}
