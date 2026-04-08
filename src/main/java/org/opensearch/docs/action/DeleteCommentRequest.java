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
import org.opensearch.core.common.Strings;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

public class DeleteCommentRequest extends ActionRequest {
  private final String commentId;
  private final Long seqNo;
  private final Long primaryTerm;

  public DeleteCommentRequest(String commentId, Long seqNo, Long primaryTerm) {
    this.commentId = commentId;
    this.seqNo = seqNo;
    this.primaryTerm = primaryTerm;
  }

  public DeleteCommentRequest(StreamInput in) throws IOException {
    super(in);
    this.commentId = in.readString();
    this.seqNo = in.readOptionalLong();
    this.primaryTerm = in.readOptionalLong();
  }

  public String getCommentId() {
    return commentId;
  }

  public long getSeqNo() {
    return seqNo == null ? -1L : seqNo;
  }

  public long getPrimaryTerm() {
    return primaryTerm == null ? -1L : primaryTerm;
  }

  @Override
  public ActionRequestValidationException validate() {
    ActionRequestValidationException validationException = null;
    if (Strings.hasText(commentId) == false) {
      validationException = addValidationError("commentId is required", validationException);
    }
    if (seqNo == null || primaryTerm == null) {
      validationException =
          addValidationError(
              "seqNo and primaryTerm are required when deleting a comment", validationException);
    }
    return validationException;
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    super.writeTo(out);
    out.writeString(commentId);
    out.writeOptionalLong(seqNo);
    out.writeOptionalLong(primaryTerm);
  }
}
