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
import org.opensearch.docs.model.FolderRecord;

public class GetFolderResponse extends ActionResponse implements ToXContentObject {
  private final FolderRecord folder;

  public GetFolderResponse(FolderRecord folder) {
    this.folder = folder;
  }

  public GetFolderResponse(StreamInput in) throws IOException {
    this(new FolderRecord(in));
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    folder.writeTo(out);
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field("folder", folder);
    builder.endObject();
    return builder;
  }
}
