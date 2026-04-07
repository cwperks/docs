/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opensearch.ResourceNotFoundException;
import org.opensearch.action.DocWriteResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.action.ActionListener;
import org.opensearch.docs.Constants;
import org.opensearch.docs.action.DeleteDocumentRequest;
import org.opensearch.docs.action.DeleteDocumentResponse;
import org.opensearch.docs.action.GetDocumentResponse;
import org.opensearch.docs.action.ListDocumentsResponse;
import org.opensearch.docs.action.UpsertDocumentRequest;
import org.opensearch.docs.action.UpsertDocumentResponse;
import org.opensearch.docs.model.DocumentRecord;
import org.opensearch.docs.model.DocumentSummary;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

public class DocumentIndexService {
  private final PluginClient pluginClient;

  @Inject
  public DocumentIndexService(PluginClient pluginClient) {
    this.pluginClient = pluginClient;
  }

  public void listDocuments(
      String query, int size, ActionListener<ListDocumentsResponse> listener) {
    indexExists(
        ActionListener.wrap(
            exists -> {
              if (exists == false) {
                listener.onResponse(new ListDocumentsResponse(List.of()));
                return;
              }

              SearchSourceBuilder sourceBuilder =
                  new SearchSourceBuilder().size(size).sort("updated_at", SortOrder.DESC);
              BoolQueryBuilder visibilityFilter =
                  QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("is_deleted", true));
              if (query == null || query.isBlank()) {
                sourceBuilder.query(visibilityFilter.must(QueryBuilders.matchAllQuery()));
              } else {
                BoolQueryBuilder boolQuery =
                    QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchPhrasePrefixQuery("title", query).boost(4.0f))
                        .should(QueryBuilders.matchQuery("content", query))
                        .minimumShouldMatch(1);
                sourceBuilder.query(visibilityFilter.must(boolQuery));
              }

              SearchRequest searchRequest =
                  new SearchRequest(Constants.DOCS_INDEX).source(sourceBuilder);
              pluginClient.search(
                  searchRequest,
                  ActionListener.wrap(
                      response -> {
                        List<DocumentSummary> documents =
                            java.util.Arrays.stream(response.getHits().getHits())
                                .map(
                                    hit ->
                                        DocumentRecord.fromSource(
                                                hit.getId(),
                                                hit.getSeqNo(),
                                                hit.getPrimaryTerm(),
                                                hit.getSourceAsMap())
                                            .toSummary())
                                .toList();
                        listener.onResponse(new ListDocumentsResponse(documents));
                      },
                      listener::onFailure));
            },
            listener::onFailure));
  }

  public void getDocument(String documentId, ActionListener<GetDocumentResponse> listener) {
    indexExists(
        ActionListener.wrap(
            exists -> {
              if (exists == false) {
                listener.onFailure(
                    new ResourceNotFoundException("Document [" + documentId + "] does not exist"));
                return;
              }

              pluginClient.get(
                  new GetRequest(Constants.DOCS_INDEX, documentId),
                  ActionListener.wrap(
                      response -> {
                        if (response.isExists() == false) {
                          listener.onFailure(
                              new ResourceNotFoundException(
                                  "Document [" + documentId + "] does not exist"));
                          return;
                        }

                        DocumentRecord document =
                            DocumentRecord.fromSource(
                                response.getId(),
                                response.getSeqNo(),
                                response.getPrimaryTerm(),
                                response.getSourceAsMap());
                        if (document.isDeleted()) {
                          listener.onFailure(
                              new ResourceNotFoundException(
                                  "Document [" + documentId + "] does not exist"));
                          return;
                        }
                        listener.onResponse(new GetDocumentResponse(document));
                      },
                      listener::onFailure));
            },
            listener::onFailure));
  }

  public void upsertDocument(
      UpsertDocumentRequest request,
      String actor,
      ActionListener<UpsertDocumentResponse> listener) {
    ensureIndexReady(
        ActionListener.wrap(
            ignored -> {
              if (request.getDocumentId() == null) {
                createDocument(request, actor, listener);
                return;
              }
              updateDocument(request, actor, listener);
            },
            listener::onFailure));
  }

  public void deleteDocument(
      DeleteDocumentRequest request,
      String actor,
      ActionListener<DeleteDocumentResponse> listener) {
    ensureIndexReady(
        ActionListener.wrap(
            ignored -> {
              pluginClient.get(
                  new GetRequest(Constants.DOCS_INDEX, request.getDocumentId()),
                  ActionListener.wrap(
                      response -> {
                        if (response.isExists() == false) {
                          listener.onFailure(
                              new ResourceNotFoundException(
                                  "Document [" + request.getDocumentId() + "] does not exist"));
                          return;
                        }

                        DocumentRecord existing =
                            DocumentRecord.fromSource(
                                response.getId(),
                                response.getSeqNo(),
                                response.getPrimaryTerm(),
                                response.getSourceAsMap());
                        if (existing.isDeleted()) {
                          listener.onFailure(
                              new ResourceNotFoundException(
                                  "Document [" + request.getDocumentId() + "] does not exist"));
                          return;
                        }

                        long now = Instant.now().toEpochMilli();
                        DocumentRecord deleted =
                            new DocumentRecord(
                                existing.getId(),
                                existing.getResourceType(),
                                existing.getAllSharedPrincipals(),
                                existing.getTitle(),
                                existing.getContent(),
                                existing.getOwner(),
                                actor,
                                existing.getCreatedAt(),
                                now,
                                true,
                                now,
                                actor,
                                request.getSeqNo(),
                                request.getPrimaryTerm());

                        IndexRequest indexRequest =
                            pluginClient
                                .prepareIndex(Constants.DOCS_INDEX)
                                .setId(existing.getId())
                                .setIfSeqNo(request.getSeqNo())
                                .setIfPrimaryTerm(request.getPrimaryTerm())
                                .setSource(toSource(deleted), XContentType.JSON)
                                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                                .request();

                        pluginClient.index(
                            indexRequest,
                            ActionListener.wrap(
                                indexResponse ->
                                    listener.onResponse(
                                        new DeleteDocumentResponse(true, indexResponse.getId())),
                                listener::onFailure));
                      },
                      listener::onFailure));
            },
            listener::onFailure));
  }

  private void createDocument(
      UpsertDocumentRequest request,
      String actor,
      ActionListener<UpsertDocumentResponse> listener) {
    long now = Instant.now().toEpochMilli();
    String documentId = UUID.randomUUID().toString();
    DocumentRecord document =
        new DocumentRecord(
            documentId,
            Constants.DOC_RESOURCE_TYPE,
            List.of(),
            request.getTitle(),
            request.getContent(),
            actor,
            actor,
            now,
            now,
            false,
            0L,
            "",
            -1L,
            -1L);

    IndexRequest indexRequest =
        pluginClient
            .prepareIndex(Constants.DOCS_INDEX)
            .setId(documentId)
            .setSource(toSource(document), XContentType.JSON)
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
            .request();

    pluginClient.index(
        indexRequest,
        ActionListener.wrap(
            response ->
                listener.onResponse(
                    new UpsertDocumentResponse(
                        response.getResult() == DocWriteResponse.Result.CREATED,
                        new DocumentRecord(
                            response.getId(),
                            document.getResourceType(),
                            document.getAllSharedPrincipals(),
                            document.getTitle(),
                            document.getContent(),
                            document.getOwner(),
                            document.getLastUpdatedBy(),
                            document.getCreatedAt(),
                            document.getUpdatedAt(),
                            document.isDeleted(),
                            document.getDeletedAt(),
                            document.getDeletedBy(),
                            response.getSeqNo(),
                            response.getPrimaryTerm()))),
            listener::onFailure));
  }

  private void updateDocument(
      UpsertDocumentRequest request,
      String actor,
      ActionListener<UpsertDocumentResponse> listener) {
    pluginClient.get(
        new GetRequest(Constants.DOCS_INDEX, request.getDocumentId()),
        ActionListener.wrap(
            response -> {
              if (response.isExists() == false) {
                listener.onFailure(
                    new ResourceNotFoundException(
                        "Document [" + request.getDocumentId() + "] does not exist"));
                return;
              }

              Map<String, Object> source = response.getSourceAsMap();
              long now = Instant.now().toEpochMilli();
              DocumentRecord existing =
                  DocumentRecord.fromSource(
                      response.getId(), response.getSeqNo(), response.getPrimaryTerm(), source);
              if (existing.isDeleted()) {
                listener.onFailure(
                    new ResourceNotFoundException(
                        "Document [" + request.getDocumentId() + "] does not exist"));
                return;
              }

              DocumentRecord updated =
                  new DocumentRecord(
                      existing.getId(),
                      existing.getResourceType(),
                      existing.getAllSharedPrincipals(),
                      request.getTitle(),
                      request.getContent(),
                      existing.getOwner(),
                      actor,
                      existing.getCreatedAt(),
                      now,
                      false,
                      0L,
                      "",
                      request.getSeqNo(),
                      request.getPrimaryTerm());

              IndexRequest indexRequest =
                  pluginClient
                      .prepareIndex(Constants.DOCS_INDEX)
                      .setId(existing.getId())
                      .setIfSeqNo(request.getSeqNo())
                      .setIfPrimaryTerm(request.getPrimaryTerm())
                      .setSource(toSource(updated), XContentType.JSON)
                      .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                      .request();

              pluginClient.index(
                  indexRequest,
                  ActionListener.wrap(
                      indexResponse ->
                          listener.onResponse(
                              new UpsertDocumentResponse(
                                  indexResponse.getResult() == DocWriteResponse.Result.CREATED,
                                  new DocumentRecord(
                                      indexResponse.getId(),
                                      updated.getResourceType(),
                                      updated.getAllSharedPrincipals(),
                                      updated.getTitle(),
                                      updated.getContent(),
                                      updated.getOwner(),
                                      updated.getLastUpdatedBy(),
                                      updated.getCreatedAt(),
                                      updated.getUpdatedAt(),
                                      updated.isDeleted(),
                                      updated.getDeletedAt(),
                                      updated.getDeletedBy(),
                                      indexResponse.getSeqNo(),
                                      indexResponse.getPrimaryTerm()))),
                      listener::onFailure));
            },
            listener::onFailure));
  }

  private void ensureIndexReady(ActionListener<Void> listener) {
    indexExists(
        ActionListener.wrap(
            exists -> {
              if (exists) {
                ensureMappings(listener);
                return;
              }

              String mappings = loadMappings();
              pluginClient
                  .admin()
                  .indices()
                  .prepareCreate(Constants.DOCS_INDEX)
                  .setSettings(
                      Settings.builder()
                          .put("index.hidden", true)
                          .put("index.number_of_shards", 1)
                          .put("index.number_of_replicas", 0))
                  .setMapping(mappings)
                  .execute(
                      ActionListener.wrap(
                          response -> {
                            if (response.isAcknowledged() == false) {
                              listener.onFailure(
                                  new IllegalStateException(
                                      "Create index was not acknowledged for "
                                          + Constants.DOCS_INDEX));
                              return;
                            }
                            ensureMappings(listener);
                          },
                          listener::onFailure));
            },
            listener::onFailure));
  }

  private void ensureMappings(ActionListener<Void> listener) {
    String mappings = loadMappings();
    pluginClient
        .admin()
        .indices()
        .preparePutMapping(Constants.DOCS_INDEX)
        .setSource(mappings, XContentType.JSON)
        .execute(
            ActionListener.wrap(
                response -> {
                  if (response.isAcknowledged() == false) {
                    listener.onFailure(
                        new IllegalStateException(
                            "Put mapping was not acknowledged for " + Constants.DOCS_INDEX));
                    return;
                  }
                  listener.onResponse(null);
                },
                listener::onFailure));
  }

  private void indexExists(ActionListener<Boolean> listener) {
    pluginClient
        .admin()
        .indices()
        .prepareExists(Constants.DOCS_INDEX)
        .execute(
            ActionListener.wrap(
                response -> listener.onResponse(response.isExists()), listener::onFailure));
  }

  private String loadMappings() {
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream(Constants.MAPPINGS_RESOURCE_PATH)) {
      if (inputStream == null) {
        throw new IllegalStateException(
            "Could not find " + Constants.MAPPINGS_RESOURCE_PATH + " on the classpath");
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load mappings for " + Constants.DOCS_INDEX, e);
    }
  }

  private String toSource(DocumentRecord document) {
    return """
            {
              "resource_type": %s,
              "all_shared_principals": %s,
              "title": %s,
              "content": %s,
              "owner": %s,
              "last_updated_by": %s,
              "created_at": %d,
              "updated_at": %d,
              "is_deleted": %s,
              "deleted_at": %d,
              "deleted_by": %s
            }
            """
        .formatted(
            quote(document.getResourceType()),
            quoteArray(document.getAllSharedPrincipals()),
            quote(document.getTitle()),
            quote(document.getContent()),
            quote(document.getOwner()),
            quote(document.getLastUpdatedBy()),
            document.getCreatedAt(),
            document.getUpdatedAt(),
            document.isDeleted(),
            document.getDeletedAt(),
            quote(document.getDeletedBy()));
  }

  private String quote(String value) {
    String sanitized =
        value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    return "\"" + sanitized + "\"";
  }

  private String quoteArray(List<String> values) {
    return values.stream()
        .map(this::quote)
        .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
  }
}
