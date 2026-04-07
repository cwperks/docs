/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs;

import java.util.Set;
import org.opensearch.security.spi.resources.ResourceProvider;
import org.opensearch.security.spi.resources.ResourceSharingExtension;
import org.opensearch.security.spi.resources.client.ResourceSharingClient;

public class DocsResourceSharingExtension implements ResourceSharingExtension {
  @Override
  public Set<ResourceProvider> getResourceProviders() {
    return Set.of(
        new ResourceProvider() {
          @Override
          public String resourceType() {
            return Constants.DOC_RESOURCE_TYPE;
          }

          @Override
          public String resourceIndexName() {
            return Constants.DOCS_INDEX;
          }

          @Override
          public String typeField() {
            return "resource_type";
          }

          @Override
          public String parentType() {
            return Constants.FOLDER_RESOURCE_TYPE;
          }

          @Override
          public String parentIdField() {
            return "folder_id";
          }
        },
        new ResourceProvider() {
          @Override
          public String resourceType() {
            return Constants.FOLDER_RESOURCE_TYPE;
          }

          @Override
          public String resourceIndexName() {
            return Constants.DOCS_INDEX;
          }

          @Override
          public String typeField() {
            return "resource_type";
          }

          @Override
          public String parentType() {
            return Constants.FOLDER_RESOURCE_TYPE;
          }

          @Override
          public String parentIdField() {
            return "parent_id";
          }
        });
  }

  @Override
  public void assignResourceSharingClient(ResourceSharingClient resourceSharingClient) {
    // The docs plugin relies on security's REST APIs and request interception for this first pass.
  }
}
