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

public class UpsertDocumentRequest extends ActionRequest implements DocRequest {
  private final String documentId;
  private final String title;
  private final String content;
  private final String folder;
  private final Long seqNo;
  private final Long primaryTerm;

  public UpsertDocumentRequest(
      String documentId,
      String title,
      String content,
      String folder,
      Long seqNo,
      Long primaryTerm) {
    this.documentId = documentId;
    this.title = title;
    this.content = content;
    this.folder = folder == null ? "" : folder.trim();
    this.seqNo = seqNo;
    this.primaryTerm = primaryTerm;
  }

  public UpsertDocumentRequest(StreamInput in) throws IOException {
    super(in);
    this.documentId = in.readOptionalString();
    this.title = in.readString();
    this.content = in.readString();
    this.folder = in.readString();
    this.seqNo = in.readOptionalLong();
    this.primaryTerm = in.readOptionalLong();
  }

  public String getDocumentId() {
    return documentId;
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
    return documentId;
  }

  @Override
  public String type() {
    return Constants.DOC_RESOURCE_TYPE;
  }

  @Override
  public ActionRequestValidationException validate() {
    ActionRequestValidationException validationException = null;
    if (Strings.hasText(title) == false) {
      validationException = addValidationError("title is required", validationException);
    }
    if (content == null) {
      validationException = addValidationError("content is required", validationException);
    }
    if (documentId != null && (seqNo == null || primaryTerm == null)) {
      validationException =
          addValidationError(
              "seqNo and primaryTerm are required when updating an existing document",
              validationException);
    }
    return validationException;
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    super.writeTo(out);
    out.writeOptionalString(documentId);
    out.writeString(title);
    out.writeString(content);
    out.writeString(folder);
    out.writeOptionalLong(seqNo);
    out.writeOptionalLong(primaryTerm);
  }
}
