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
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

public class ListDocumentsRequest extends ActionRequest {
  private final String query;
  private final int size;

  public ListDocumentsRequest(String query, int size) {
    this.query = query;
    this.size = size;
  }

  public ListDocumentsRequest(StreamInput in) throws IOException {
    super(in);
    this.query = in.readOptionalString();
    this.size = in.readInt();
  }

  public String getQuery() {
    return query;
  }

  public int getSize() {
    return size;
  }

  @Override
  public ActionRequestValidationException validate() {
    return null;
  }

  @Override
  public void writeTo(StreamOutput out) throws IOException {
    super.writeTo(out);
    out.writeOptionalString(query);
    out.writeInt(size);
  }
}
