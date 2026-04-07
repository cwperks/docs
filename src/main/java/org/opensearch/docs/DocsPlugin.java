/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.docs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.opensearch.action.ActionRequest;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.node.DiscoveryNodes;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.IndexScopedSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsFilter;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.NamedWriteableRegistry;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.docs.action.DeleteDocumentAction;
import org.opensearch.docs.action.DeleteDocumentTransportAction;
import org.opensearch.docs.action.GetDocumentAction;
import org.opensearch.docs.action.GetDocumentTransportAction;
import org.opensearch.docs.action.ListDocumentsAction;
import org.opensearch.docs.action.ListDocumentsTransportAction;
import org.opensearch.docs.action.UpsertDocumentAction;
import org.opensearch.docs.action.UpsertDocumentTransportAction;
import org.opensearch.docs.rest.DeleteDocumentRestAction;
import org.opensearch.docs.rest.GetDocumentRestAction;
import org.opensearch.docs.rest.ListDocumentsRestAction;
import org.opensearch.docs.rest.UpsertDocumentRestAction;
import org.opensearch.docs.service.DocumentIndexService;
import org.opensearch.docs.service.PluginClient;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.identity.PluginSubject;
import org.opensearch.indices.SystemIndexDescriptor;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.IdentityAwarePlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.SystemIndexPlugin;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestHandler;
import org.opensearch.script.ScriptService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.client.Client;
import org.opensearch.watcher.ResourceWatcherService;

public class DocsPlugin extends Plugin
    implements ActionPlugin, SystemIndexPlugin, IdentityAwarePlugin {
  private PluginClient pluginClient;

  @Override
  public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
    return Collections.singletonList(
        new SystemIndexDescriptor(
            Constants.DOCS_INDEX, "Stores collaborative documents for the OpenSearch Docs plugin"));
  }

  @Override
  public Collection<Object> createComponents(
      Client client,
      ClusterService clusterService,
      ThreadPool threadPool,
      ResourceWatcherService resourceWatcherService,
      ScriptService scriptService,
      NamedXContentRegistry xContentRegistry,
      Environment environment,
      NodeEnvironment nodeEnvironment,
      NamedWriteableRegistry namedWriteableRegistry,
      IndexNameExpressionResolver indexNameExpressionResolver,
      Supplier<RepositoriesService> repositoriesServiceSupplier) {
    this.pluginClient = new PluginClient(client);
    DocumentIndexService documentIndexService = new DocumentIndexService(pluginClient);
    return List.of(pluginClient, documentIndexService);
  }

  @Override
  public List<RestHandler> getRestHandlers(
      Settings settings,
      RestController restController,
      ClusterSettings clusterSettings,
      IndexScopedSettings indexScopedSettings,
      SettingsFilter settingsFilter,
      IndexNameExpressionResolver indexNameExpressionResolver,
      Supplier<DiscoveryNodes> nodesInCluster) {
    List<RestHandler> handlers = new ArrayList<>();
    handlers.add(new ListDocumentsRestAction());
    handlers.add(new GetDocumentRestAction());
    handlers.add(new UpsertDocumentRestAction());
    handlers.add(new DeleteDocumentRestAction());
    return handlers;
  }

  @Override
  public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
    List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> actions =
        new ArrayList<>();
    actions.add(
        new ActionHandler<>(ListDocumentsAction.INSTANCE, ListDocumentsTransportAction.class));
    actions.add(new ActionHandler<>(GetDocumentAction.INSTANCE, GetDocumentTransportAction.class));
    actions.add(
        new ActionHandler<>(UpsertDocumentAction.INSTANCE, UpsertDocumentTransportAction.class));
    actions.add(
        new ActionHandler<>(DeleteDocumentAction.INSTANCE, DeleteDocumentTransportAction.class));
    return actions;
  }

  @Override
  public void assignSubject(PluginSubject pluginSubject) {
    if (pluginClient != null) {
      pluginClient.setSubject(pluginSubject);
    }
  }
}
