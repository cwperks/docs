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
import java.util.UUID;
import java.util.stream.Collectors;
import org.opensearch.ResourceNotFoundException;
import org.opensearch.action.DocWriteResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.support.GroupedActionListener;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.action.ActionListener;
import org.opensearch.docs.Constants;
import org.opensearch.docs.action.DeleteDocumentRequest;
import org.opensearch.docs.action.DeleteDocumentResponse;
import org.opensearch.docs.action.DeleteFolderRequest;
import org.opensearch.docs.action.DeleteFolderResponse;
import org.opensearch.docs.action.GetDocumentResponse;
import org.opensearch.docs.action.GetFolderResponse;
import org.opensearch.docs.action.ListDocumentsResponse;
import org.opensearch.docs.action.ListFoldersResponse;
import org.opensearch.docs.action.UpsertDocumentRequest;
import org.opensearch.docs.action.UpsertDocumentResponse;
import org.opensearch.docs.action.UpsertFolderRequest;
import org.opensearch.docs.action.UpsertFolderResponse;
import org.opensearch.docs.model.DocumentRecord;
import org.opensearch.docs.model.DocumentSummary;
import org.opensearch.docs.model.FolderRecord;
import org.opensearch.docs.model.FolderSummary;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
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
              BoolQueryBuilder visibilityFilter = activeResourceFilter(Constants.DOC_RESOURCE_TYPE);
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
                        if (document.isDeleted()
                            || Constants.DOC_RESOURCE_TYPE.equals(document.getResourceType())
                                == false) {
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
                        if (existing.isDeleted()
                            || Constants.DOC_RESOURCE_TYPE.equals(existing.getResourceType())
                                == false) {
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
                                existing.getFolderId(),
                                existing.getFolderPath(),
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

  public void listFolders(String query, int size, ActionListener<ListFoldersResponse> listener) {
    indexExists(
        ActionListener.wrap(
            exists -> {
              if (exists == false) {
                listener.onResponse(new ListFoldersResponse(List.of()));
                return;
              }

              SearchSourceBuilder sourceBuilder =
                  new SearchSourceBuilder()
                      .size(size)
                      .sort("path.keyword", SortOrder.ASC)
                      .sort("updated_at", SortOrder.DESC);
              BoolQueryBuilder visibilityFilter =
                  activeResourceFilter(Constants.FOLDER_RESOURCE_TYPE);
              if (query == null || query.isBlank()) {
                sourceBuilder.query(visibilityFilter.must(QueryBuilders.matchAllQuery()));
              } else {
                BoolQueryBuilder boolQuery =
                    QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchPhrasePrefixQuery("name", query).boost(4.0f))
                        .should(QueryBuilders.matchPhrasePrefixQuery("path", query).boost(2.0f))
                        .minimumShouldMatch(1);
                sourceBuilder.query(visibilityFilter.must(boolQuery));
              }

              SearchRequest searchRequest =
                  new SearchRequest(Constants.DOCS_INDEX).source(sourceBuilder);
              pluginClient.search(
                  searchRequest,
                  ActionListener.wrap(
                      response -> {
                        List<FolderSummary> folders =
                            java.util.Arrays.stream(response.getHits().getHits())
                                .map(
                                    hit ->
                                        FolderRecord.fromSource(
                                                hit.getId(),
                                                hit.getSeqNo(),
                                                hit.getPrimaryTerm(),
                                                hit.getSourceAsMap())
                                            .toSummary())
                                .toList();
                        listener.onResponse(new ListFoldersResponse(folders));
                      },
                      listener::onFailure));
            },
            listener::onFailure));
  }

  public void getFolder(String folderId, ActionListener<GetFolderResponse> listener) {
    indexExists(
        ActionListener.wrap(
            exists -> {
              if (exists == false) {
                listener.onFailure(
                    new ResourceNotFoundException("Folder [" + folderId + "] does not exist"));
                return;
              }

              pluginClient.get(
                  new GetRequest(Constants.DOCS_INDEX, folderId),
                  ActionListener.wrap(
                      response -> {
                        if (response.isExists() == false) {
                          listener.onFailure(
                              new ResourceNotFoundException(
                                  "Folder [" + folderId + "] does not exist"));
                          return;
                        }

                        FolderRecord folder =
                            FolderRecord.fromSource(
                                response.getId(),
                                response.getSeqNo(),
                                response.getPrimaryTerm(),
                                response.getSourceAsMap());
                        if (folder.isDeleted()
                            || Constants.FOLDER_RESOURCE_TYPE.equals(folder.getResourceType())
                                == false) {
                          listener.onFailure(
                              new ResourceNotFoundException(
                                  "Folder [" + folderId + "] does not exist"));
                          return;
                        }
                        listener.onResponse(new GetFolderResponse(folder));
                      },
                      listener::onFailure));
            },
            listener::onFailure));
  }

  public void upsertFolder(
      UpsertFolderRequest request, String actor, ActionListener<UpsertFolderResponse> listener) {
    ensureIndexReady(
        ActionListener.wrap(
            ignored -> {
              if (request.getFolderId() == null) {
                createFolder(request, actor, listener);
                return;
              }
              updateFolder(request, actor, listener);
            },
            listener::onFailure));
  }

  public void deleteFolder(
      DeleteFolderRequest request, String actor, ActionListener<DeleteFolderResponse> listener) {
    ensureIndexReady(
        ActionListener.wrap(
            ignored ->
                getFolderRecord(
                    request.getFolderId(),
                    ActionListener.wrap(
                        existing ->
                            softDeleteFolderRecursive(
                                existing,
                                actor,
                                request.getSeqNo(),
                                request.getPrimaryTerm(),
                                listener),
                        listener::onFailure)),
            listener::onFailure));
  }

  private void softDeleteFolderRecursive(
      FolderRecord folder,
      String actor,
      long seqNo,
      long primaryTerm,
      ActionListener<DeleteFolderResponse> listener) {
    // First, soft-delete all documents in this folder
    softDeleteDocumentsInFolder(
        folder.getPath(),
        actor,
        ActionListener.wrap(
            docsDeleted -> {
              // Then, find and recursively delete child folders
              findChildFolders(
                  folder.getId(),
                  folder.getPath(),
                  ActionListener.wrap(
                      childFolders -> {
                        if (childFolders.isEmpty()) {
                          // No children — delete this folder
                          performFolderSoftDelete(folder, actor, seqNo, primaryTerm, listener);
                          return;
                        }
                        // Recursively delete each child folder, then delete this one
                        deleteChildFoldersSequentially(
                            childFolders,
                            0,
                            actor,
                            ActionListener.wrap(
                                allDeleted ->
                                    performFolderSoftDelete(
                                        folder, actor, seqNo, primaryTerm, listener),
                                listener::onFailure));
                      },
                      listener::onFailure));
            },
            listener::onFailure));
  }

  private void deleteChildFoldersSequentially(
      List<FolderRecord> children, int index, String actor, ActionListener<Void> listener) {
    if (index >= children.size()) {
      listener.onResponse(null);
      return;
    }
    FolderRecord child = children.get(index);
    softDeleteFolderRecursive(
        child,
        actor,
        child.getSeqNo(),
        child.getPrimaryTerm(),
        ActionListener.wrap(
            deleted -> deleteChildFoldersSequentially(children, index + 1, actor, listener),
            listener::onFailure));
  }

  private void softDeleteDocumentsInFolder(
      String folderPath, String actor, ActionListener<Void> listener) {
    BoolQueryBuilder query =
        activeResourceFilter(Constants.DOC_RESOURCE_TYPE)
            .must(QueryBuilders.termQuery("folder_path", folderPath));
    pluginClient.search(
        new SearchRequest(Constants.DOCS_INDEX)
            .source(new SearchSourceBuilder().size(200).query(query)),
        ActionListener.wrap(
            response -> {
              var hits = response.getHits().getHits();
              if (hits.length == 0) {
                listener.onResponse(null);
                return;
              }
              long now = Instant.now().toEpochMilli();
              GroupedActionListener<Void> groupListener =
                  new GroupedActionListener<>(
                      ActionListener.wrap(
                          results -> listener.onResponse(null), listener::onFailure),
                      hits.length);
              for (var hit : hits) {
                DocumentRecord doc =
                    DocumentRecord.fromSource(
                        hit.getId(), hit.getSeqNo(), hit.getPrimaryTerm(), hit.getSourceAsMap());
                DocumentRecord deleted =
                    new DocumentRecord(
                        doc.getId(),
                        doc.getResourceType(),
                        doc.getAllSharedPrincipals(),
                        doc.getTitle(),
                        doc.getContent(),
                        doc.getFolderId(),
                        doc.getFolderPath(),
                        doc.getOwner(),
                        actor,
                        doc.getCreatedAt(),
                        now,
                        true,
                        now,
                        actor,
                        doc.getSeqNo(),
                        doc.getPrimaryTerm());
                IndexRequest ir =
                    pluginClient
                        .prepareIndex(Constants.DOCS_INDEX)
                        .setId(doc.getId())
                        .setIfSeqNo(doc.getSeqNo())
                        .setIfPrimaryTerm(doc.getPrimaryTerm())
                        .setSource(toSource(deleted), XContentType.JSON)
                        .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                        .request();
                pluginClient.index(
                    ir,
                    ActionListener.wrap(
                        r -> groupListener.onResponse(null), groupListener::onFailure));
              }
            },
            listener::onFailure));
  }

  private void findChildFolders(
      String parentId, String parentPath, ActionListener<List<FolderRecord>> listener) {
    BoolQueryBuilder query =
        activeResourceFilter(Constants.FOLDER_RESOURCE_TYPE)
            .must(
                QueryBuilders.boolQuery()
                    .should(QueryBuilders.termQuery("parent_id", parentId))
                    .should(QueryBuilders.prefixQuery("path.keyword", parentPath + "/"))
                    .minimumShouldMatch(1));
    pluginClient.search(
        new SearchRequest(Constants.DOCS_INDEX)
            .source(new SearchSourceBuilder().size(200).query(query)),
        ActionListener.wrap(
            response -> {
              List<FolderRecord> folders =
                  java.util.Arrays.stream(response.getHits().getHits())
                      .map(
                          hit ->
                              FolderRecord.fromSource(
                                  hit.getId(),
                                  hit.getSeqNo(),
                                  hit.getPrimaryTerm(),
                                  hit.getSourceAsMap()))
                      .filter(f -> !f.getId().equals(parentId))
                      .toList();
              listener.onResponse(folders);
            },
            listener::onFailure));
  }

  private void performFolderSoftDelete(
      FolderRecord existing,
      String actor,
      long seqNo,
      long primaryTerm,
      ActionListener<DeleteFolderResponse> listener) {
    // Re-fetch to get latest seqNo/primaryTerm (may have changed due to visibility updates)
    getFolderRecord(
        existing.getId(),
        ActionListener.wrap(
            fresh -> {
              long now = Instant.now().toEpochMilli();
              FolderRecord deleted =
                  new FolderRecord(
                      fresh.getId(),
                      fresh.getResourceType(),
                      fresh.getAllSharedPrincipals(),
                      fresh.getName(),
                      fresh.getPath(),
                      fresh.getParentId(),
                      fresh.getOwner(),
                      actor,
                      fresh.getCreatedAt(),
                      now,
                      true,
                      now,
                      actor,
                      fresh.getSeqNo(),
                      fresh.getPrimaryTerm());
              IndexRequest indexRequest =
                  pluginClient
                      .prepareIndex(Constants.DOCS_INDEX)
                      .setId(fresh.getId())
                      .setIfSeqNo(fresh.getSeqNo())
                      .setIfPrimaryTerm(fresh.getPrimaryTerm())
                      .setSource(toSource(deleted), XContentType.JSON)
                      .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                      .request();
              pluginClient.index(
                  indexRequest,
                  ActionListener.wrap(
                      indexResponse ->
                          listener.onResponse(new DeleteFolderResponse(true, indexResponse.getId())),
                      listener::onFailure));
            },
            listener::onFailure));
  }

  private void createDocument(
      UpsertDocumentRequest request,
      String actor,
      ActionListener<UpsertDocumentResponse> listener) {
    resolveFolderRecord(
        request.getFolderId(),
        ActionListener.wrap(
            folder -> {
              long now = Instant.now().toEpochMilli();
              String documentId = UUID.randomUUID().toString();
              DocumentRecord document =
                  new DocumentRecord(
                      documentId,
                      Constants.DOC_RESOURCE_TYPE,
                      folder == null ? List.of() : folder.getAllSharedPrincipals(),
                      request.getTitle(),
                      request.getContent(),
                      folder == null ? "" : folder.getId(),
                      folder == null ? "" : folder.getPath(),
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
                                      document.getFolderId(),
                                      document.getFolderPath(),
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
            },
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

              long now = Instant.now().toEpochMilli();
              DocumentRecord existing =
                  DocumentRecord.fromSource(
                      response.getId(),
                      response.getSeqNo(),
                      response.getPrimaryTerm(),
                      response.getSourceAsMap());
              if (existing.isDeleted()
                  || Constants.DOC_RESOURCE_TYPE.equals(existing.getResourceType()) == false) {
                listener.onFailure(
                    new ResourceNotFoundException(
                        "Document [" + request.getDocumentId() + "] does not exist"));
                return;
              }

              resolveFolderRecord(
                  request.getFolderId(),
                  ActionListener.wrap(
                      folder -> {
                        boolean movedOutOfFolder =
                            existing.getFolderId().isBlank() == false
                                && (folder == null
                                    || existing.getFolderId().equals(folder.getId()) == false);
                        List<String> principals =
                            folder != null
                                ? folder.getAllSharedPrincipals()
                                : movedOutOfFolder ? List.of() : existing.getAllSharedPrincipals();

                        DocumentRecord updated =
                            new DocumentRecord(
                                existing.getId(),
                                existing.getResourceType(),
                                principals,
                                request.getTitle(),
                                request.getContent(),
                                folder == null ? "" : folder.getId(),
                                folder == null ? "" : folder.getPath(),
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
                                            indexResponse.getResult()
                                                == DocWriteResponse.Result.CREATED,
                                            new DocumentRecord(
                                                indexResponse.getId(),
                                                updated.getResourceType(),
                                                updated.getAllSharedPrincipals(),
                                                updated.getTitle(),
                                                updated.getContent(),
                                                updated.getFolderId(),
                                                updated.getFolderPath(),
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
            },
            listener::onFailure));
  }

  private void createFolder(
      UpsertFolderRequest request, String actor, ActionListener<UpsertFolderResponse> listener) {
    resolveParentFolder(
        request.getParentId(),
        ActionListener.wrap(
            parent -> {
              String path = buildFolderPath(parent, request.getName());
              ensureFolderPathAvailable(
                  path,
                  null,
                  ActionListener.wrap(
                      ignored -> {
                        long now = Instant.now().toEpochMilli();
                        String folderId = UUID.randomUUID().toString();
                        FolderRecord folder =
                            new FolderRecord(
                                folderId,
                                Constants.FOLDER_RESOURCE_TYPE,
                                List.of(),
                                request.getName(),
                                path,
                                parent == null ? "" : parent.getId(),
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
                                .setId(folderId)
                                .setSource(toSource(folder), XContentType.JSON)
                                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                                .request();

                        pluginClient.index(
                            indexRequest,
                            ActionListener.wrap(
                                response ->
                                    listener.onResponse(
                                        new UpsertFolderResponse(
                                            response.getResult() == DocWriteResponse.Result.CREATED,
                                            new FolderRecord(
                                                response.getId(),
                                                folder.getResourceType(),
                                                folder.getAllSharedPrincipals(),
                                                folder.getName(),
                                                folder.getPath(),
                                                folder.getParentId(),
                                                folder.getOwner(),
                                                folder.getLastUpdatedBy(),
                                                folder.getCreatedAt(),
                                                folder.getUpdatedAt(),
                                                folder.isDeleted(),
                                                folder.getDeletedAt(),
                                                folder.getDeletedBy(),
                                                response.getSeqNo(),
                                                response.getPrimaryTerm()))),
                                listener::onFailure));
                      },
                      listener::onFailure));
            },
            listener::onFailure));
  }

  private void updateFolder(
      UpsertFolderRequest request, String actor, ActionListener<UpsertFolderResponse> listener) {
    getFolderRecord(
        request.getFolderId(),
        ActionListener.wrap(
            existing ->
                resolveParentFolder(
                    request.getParentId(),
                    ActionListener.wrap(
                        parent -> {
                          if (parent != null && parent.getId().equals(existing.getId())) {
                            listener.onFailure(
                                new IllegalArgumentException("Folder cannot be moved into itself"));
                            return;
                          }
                          if (parent != null
                              && isNestedPath(parent.getPath(), existing.getPath())) {
                            listener.onFailure(
                                new IllegalArgumentException(
                                    "Folder cannot be moved into one of its descendants"));
                            return;
                          }

                          String newPath = buildFolderPath(parent, request.getName());
                          boolean pathChanged = existing.getPath().equals(newPath) == false;
                          ActionListener<Void> continueUpdate =
                              ActionListener.wrap(
                                  ignored ->
                                      ensureFolderPathAvailable(
                                          newPath,
                                          existing.getId(),
                                          ActionListener.wrap(
                                              ignoredAvailability -> {
                                                long now = Instant.now().toEpochMilli();
                                                FolderRecord updated =
                                                    new FolderRecord(
                                                        existing.getId(),
                                                        existing.getResourceType(),
                                                        existing.getAllSharedPrincipals(),
                                                        request.getName(),
                                                        newPath,
                                                        parent == null ? "" : parent.getId(),
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
                                                        .setSource(
                                                            toSource(updated), XContentType.JSON)
                                                        .setRefreshPolicy(
                                                            WriteRequest.RefreshPolicy.IMMEDIATE)
                                                        .request();

                                                pluginClient.index(
                                                    indexRequest,
                                                    ActionListener.wrap(
                                                        response ->
                                                            listener.onResponse(
                                                                new UpsertFolderResponse(
                                                                    response.getResult()
                                                                        == DocWriteResponse.Result
                                                                            .CREATED,
                                                                    new FolderRecord(
                                                                        response.getId(),
                                                                        updated.getResourceType(),
                                                                        updated
                                                                            .getAllSharedPrincipals(),
                                                                        updated.getName(),
                                                                        updated.getPath(),
                                                                        updated.getParentId(),
                                                                        updated.getOwner(),
                                                                        updated.getLastUpdatedBy(),
                                                                        updated.getCreatedAt(),
                                                                        updated.getUpdatedAt(),
                                                                        updated.isDeleted(),
                                                                        updated.getDeletedAt(),
                                                                        updated.getDeletedBy(),
                                                                        response.getSeqNo(),
                                                                        response
                                                                            .getPrimaryTerm()))),
                                                        listener::onFailure));
                                              },
                                              listener::onFailure)),
                                  listener::onFailure);

                          if (pathChanged == false) {
                            continueUpdate.onResponse(null);
                            return;
                          }

                          ensureFolderCanChangePath(
                              existing,
                              ActionListener.wrap(
                                  ignored -> continueUpdate.onResponse(null), listener::onFailure));
                        },
                        listener::onFailure)),
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

  private void resolveFolderRecord(String folderId, ActionListener<FolderRecord> listener) {
    if (folderId == null || folderId.isBlank()) {
      listener.onResponse(null);
      return;
    }

    getFolderRecord(folderId, listener);
  }

  private void getFolderRecord(String folderId, ActionListener<FolderRecord> listener) {
    pluginClient.get(
        new GetRequest(Constants.DOCS_INDEX, folderId),
        ActionListener.wrap(
            response -> {
              if (response.isExists() == false) {
                listener.onFailure(
                    new ResourceNotFoundException("Folder [" + folderId + "] does not exist"));
                return;
              }

              FolderRecord folder =
                  FolderRecord.fromSource(
                      response.getId(),
                      response.getSeqNo(),
                      response.getPrimaryTerm(),
                      response.getSourceAsMap());
              if (folder.isDeleted()
                  || Constants.FOLDER_RESOURCE_TYPE.equals(folder.getResourceType()) == false) {
                listener.onFailure(
                    new ResourceNotFoundException("Folder [" + folderId + "] does not exist"));
                return;
              }

              listener.onResponse(folder);
            },
            listener::onFailure));
  }

  private void resolveParentFolder(String parentId, ActionListener<FolderRecord> listener) {
    if (parentId == null || parentId.isBlank()) {
      listener.onResponse(null);
      return;
    }
    getFolderRecord(parentId, listener);
  }

  private void ensureFolderPathAvailable(
      String path, String existingFolderId, ActionListener<Void> listener) {
    SearchSourceBuilder sourceBuilder =
        new SearchSourceBuilder()
            .size(10)
            .query(
                activeResourceFilter(Constants.FOLDER_RESOURCE_TYPE)
                    .must(QueryBuilders.termQuery("path.keyword", path)));
    SearchRequest searchRequest = new SearchRequest(Constants.DOCS_INDEX).source(sourceBuilder);
    pluginClient.search(
        searchRequest,
        ActionListener.wrap(
            response -> {
              boolean hasConflict =
                  java.util.Arrays.stream(response.getHits().getHits())
                      .anyMatch(
                          hit ->
                              existingFolderId == null
                                  || existingFolderId.equals(hit.getId()) == false);
              if (hasConflict) {
                listener.onFailure(
                    new IllegalArgumentException("Folder path [" + path + "] already exists"));
                return;
              }
              listener.onResponse(null);
            },
            listener::onFailure));
  }

  private void ensureFolderCanChangePath(FolderRecord existing, ActionListener<Void> listener) {
    ensureFolderHasChildren(
        existing,
        ActionListener.wrap(
            hasChildren -> {
              if (hasChildren) {
                listener.onFailure(
                    new IllegalStateException(
                        "Folder ["
                            + existing.getPath()
                            + "] cannot be renamed or moved while it contains child folders"));
                return;
              }

              ensureFolderHasDocuments(
                  existing.getPath(),
                  ActionListener.wrap(
                      hasDocuments -> {
                        if (hasDocuments) {
                          listener.onFailure(
                              new IllegalStateException(
                                  "Folder ["
                                      + existing.getPath()
                                      + "] cannot be renamed or moved while it contains documents"));
                          return;
                        }
                        listener.onResponse(null);
                      },
                      listener::onFailure));
            },
            listener::onFailure));
  }

  private void ensureFolderHasChildren(FolderRecord folder, ActionListener<Boolean> listener) {
    BoolQueryBuilder query =
        activeResourceFilter(Constants.FOLDER_RESOURCE_TYPE)
            .must(
                QueryBuilders.boolQuery()
                    .should(QueryBuilders.termQuery("parent_id", folder.getId()))
                    .should(QueryBuilders.prefixQuery("path.keyword", folder.getPath() + "/"))
                    .minimumShouldMatch(1));
    pluginClient.search(
        new SearchRequest(Constants.DOCS_INDEX)
            .source(new SearchSourceBuilder().size(1).query(query)),
        ActionListener.wrap(
            response -> listener.onResponse(response.getHits().getHits().length > 0),
            listener::onFailure));
  }

  private void ensureFolderHasNoChildren(FolderRecord folder, ActionListener<Boolean> listener) {
    ensureFolderHasChildren(folder, listener);
  }

  private void ensureFolderHasDocuments(String folderPath, ActionListener<Boolean> listener) {
    QueryBuilder folderPathQuery =
        QueryBuilders.boolQuery()
            .should(QueryBuilders.termQuery("folder_path", folderPath))
            .should(QueryBuilders.prefixQuery("folder_path", folderPath + "/"))
            .minimumShouldMatch(1);
    BoolQueryBuilder query =
        activeResourceFilter(Constants.DOC_RESOURCE_TYPE).must(folderPathQuery);
    pluginClient.search(
        new SearchRequest(Constants.DOCS_INDEX)
            .source(new SearchSourceBuilder().size(1).query(query)),
        ActionListener.wrap(
            response -> listener.onResponse(response.getHits().getHits().length > 0),
            listener::onFailure));
  }

  private String buildFolderPath(FolderRecord parent, String folderName) {
    String normalizedName = folderName == null ? "" : folderName.trim();
    if (parent == null) {
      return normalizedName;
    }
    return parent.getPath() + "/" + normalizedName;
  }

  private boolean isNestedPath(String candidateParentPath, String folderPath) {
    return candidateParentPath.equals(folderPath)
        || candidateParentPath.startsWith(folderPath + "/");
  }

  private BoolQueryBuilder activeResourceFilter(String resourceType) {
    return QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("resource_type", resourceType))
        .mustNot(QueryBuilders.termQuery("is_deleted", true));
  }

  private String toSource(DocumentRecord document) {
    return """
            {
              "resource_type": %s,
              "all_shared_principals": %s,
              "title": %s,
              "content": %s,
              "folder_id": %s,
              "folder_path": %s,
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
            quote(document.getFolderId()),
            quote(document.getFolderPath()),
            quote(document.getOwner()),
            quote(document.getLastUpdatedBy()),
            document.getCreatedAt(),
            document.getUpdatedAt(),
            document.isDeleted(),
            document.getDeletedAt(),
            quote(document.getDeletedBy()));
  }

  private String toSource(FolderRecord folder) {
    return """
            {
              "resource_type": %s,
              "all_shared_principals": %s,
              "name": %s,
              "path": %s,
              "parent_id": %s,
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
            quote(folder.getResourceType()),
            quoteArray(folder.getAllSharedPrincipals()),
            quote(folder.getName()),
            quote(folder.getPath()),
            quote(folder.getParentId()),
            quote(folder.getOwner()),
            quote(folder.getLastUpdatedBy()),
            folder.getCreatedAt(),
            folder.getUpdatedAt(),
            folder.isDeleted(),
            folder.getDeletedAt(),
            quote(folder.getDeletedBy()));
  }

  private String quote(String value) {
    String sanitized =
        value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    return "\"" + sanitized + "\"";
  }

  private String quoteArray(List<String> values) {
    return values.stream().map(this::quote).collect(Collectors.joining(", ", "[", "]"));
  }
}
