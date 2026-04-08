/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.action;

import java.io.IOException;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.docs.model.CommentRecord;

public class CreateCommentResponse extends ActionResponse implements ToXContentObject {
  private final CommentRecord comment;

  public CreateCommentResponse(CommentRecord comment) {
    this.comment = comment;
  }

  public CreateCommentResponse(StreamInput in) throws IOException {
    this(new CommentRecord(in));
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    comment.writeTo(out);
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field("comment", comment);
    builder.endObject();
    return builder;
  }
}
