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

public class CreateCommentRequest extends ActionRequest implements DocRequest {
  private final String documentId;
  private final String threadId;
  private final String commentText;
  private final Integer startOffset;
  private final Integer endOffset;

  public CreateCommentRequest(
      String documentId,
      String threadId,
      String commentText,
      Integer startOffset,
      Integer endOffset) {
    this.documentId = documentId;
    this.threadId = threadId;
    this.commentText = commentText;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
  }

  public CreateCommentRequest(StreamInput in) throws IOException {
    super(in);
    this.documentId = in.readString();
    this.threadId = in.readOptionalString();
    this.commentText = in.readString();
    this.startOffset = in.readOptionalVInt();
    this.endOffset = in.readOptionalVInt();
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

  public Integer getStartOffset() {
    return startOffset;
  }

  public Integer getEndOffset() {
    return endOffset;
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
    if (Strings.hasText(documentId) == false) {
      validationException = addValidationError("documentId is required", validationException);
    }
    if (Strings.hasText(commentText) == false) {
      validationException = addValidationError("commentText is required", validationException);
    }
    return validationException;
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    super.writeTo(out);
    out.writeString(documentId);
    out.writeOptionalString(threadId);
    out.writeString(commentText);
    out.writeOptionalVInt(startOffset);
    out.writeOptionalVInt(endOffset);
  }
}
