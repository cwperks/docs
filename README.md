# OpenSearch Docs

`opensearch-docs` is a new OpenSearch plugin that backs a lightweight collaborative writing experience in OpenSearch Dashboards.

The initial target is intentionally narrow:

- Quip-style shared notes rather than Google-Docs-style multiplayer cursors.
- Markdown-ish plain text editing with rich preview.
- Optimistic concurrency and polling-based collaboration.
- A dedicated system index instead of Dashboards saved objects.
- Resource-sharing integration left for the next increment.

## Bootstrap Scope

This repo now contains a thin but working backend skeleton for:

- Registering a protected system index: `.opensearch-docs`
- Creating and updating documents with optimistic concurrency
- Listing recent documents for the Dashboards sidebar
- Fetching individual documents for editing and preview

## REST API

All OpenSearch endpoints are rooted at `/_plugins/_docs`.

- `GET /_plugins/_docs/documents`
- `GET /_plugins/_docs/documents/{document_id}`
- `PUT /_plugins/_docs/documents`
- `POST /_plugins/_docs/documents/{document_id}`

Create and update payloads currently accept:

```json
{
  "title": "Quarterly launch notes",
  "content": "# Heading\nWrite here...",
  "seqNo": 12,
  "primaryTerm": 1
}
```

`seqNo` and `primaryTerm` are optional on create and required on update.

## System Index

Documents are stored in the `.opensearch-docs` system index. The first write lazily creates the index with a simple mapping optimized for the initial demo.

## Next Steps

- Register the document resource type with the security plugin’s sharing extensibility APIs.
- Add document delete/archive flows.
- Add richer markdown features and slash-command style editing affordances.
- Add real presence indicators if we want a more live-collaboration demo later.
