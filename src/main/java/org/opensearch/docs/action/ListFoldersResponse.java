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
import java.util.List;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.docs.model.FolderSummary;

public class ListFoldersResponse extends ActionResponse implements ToXContentObject {
  private final List<FolderSummary> folders;

  public ListFoldersResponse(List<FolderSummary> folders) {
    this.folders = folders;
  }

  public ListFoldersResponse(StreamInput in) throws IOException {
    this(in.readList(FolderSummary::new));
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    out.writeCollection(folders);
  }

  @Override
  public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
    builder.startObject();
    builder.field("folders", folders);
    builder.endObject();
    return builder;
  }
}
