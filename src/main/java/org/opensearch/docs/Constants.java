/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.docs;

public final class Constants {
    public static final String PLUGIN_NAME = "opensearch-docs";
    public static final String DOCS_INDEX = ".opensearch-docs";
    public static final String DOCS_API_PREFIX = "/_plugins/_docs";
    public static final String DOCUMENTS_API_PATH = DOCS_API_PREFIX + "/documents";
    public static final String MAPPINGS_RESOURCE_PATH = "documents-index-mappings.json";

    private Constants() {}
}
