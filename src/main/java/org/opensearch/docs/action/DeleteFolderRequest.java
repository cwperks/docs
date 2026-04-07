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

public class DeleteFolderRequest extends ActionRequest implements DocRequest {
  private final String folderId;
  private final Long seqNo;
  private final Long primaryTerm;

  public DeleteFolderRequest(String folderId, Long seqNo, Long primaryTerm) {
    this.folderId = folderId;
    this.seqNo = seqNo;
    this.primaryTerm = primaryTerm;
  }

  public DeleteFolderRequest(StreamInput in) throws IOException {
    super(in);
    this.folderId = in.readString();
    this.seqNo = in.readOptionalLong();
    this.primaryTerm = in.readOptionalLong();
  }

  public String getFolderId() {
    return folderId;
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
    if (Strings.hasText(folderId) == false) {
      validationException = addValidationError("folderId is required", validationException);
    }
    if (seqNo == null || primaryTerm == null) {
      validationException =
          addValidationError(
              "seqNo and primaryTerm are required when deleting a folder", validationException);
    }
    return validationException;
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    super.writeTo(out);
    out.writeString(folderId);
    out.writeOptionalLong(seqNo);
    out.writeOptionalLong(primaryTerm);
  }
}
