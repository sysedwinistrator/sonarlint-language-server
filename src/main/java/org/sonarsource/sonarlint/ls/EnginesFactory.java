/*
 * SonarLint Language Server
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.sonarlint.ls;

import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.ClientModulesProvider;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.commons.log.SonarLintLogger;
import org.sonarsource.sonarlint.ls.log.LanguageClientLogOutput;
import org.sonarsource.sonarlint.ls.settings.ServerConnectionSettings;

public class EnginesFactory {

  public static Path sonarLintUserHomeOverride = null;

  private static final SonarLintLogger LOG = SonarLintLogger.get();

  private final LanguageClientLogOutput logOutput;
  private final Collection<Path> standaloneAnalyzers;
  private final Map<String, Path> embeddedPluginsToPath;
  private static final Language[] STANDALONE_LANGUAGES = {
    Language.CPP,
    Language.C,
    Language.CSS,
    Language.GO,
    Language.HTML,
    Language.IPYTHON,
    Language.JAVA,
    Language.JS,
    Language.PHP,
    Language.PYTHON,
    Language.SECRETS,
    Language.TS,
    Language.XML,
    Language.YAML,
  };

  private static final Language[] CONNECTED_ADDITIONAL_LANGUAGES = {
    Language.APEX,
    Language.PLSQL
  };

  private final NodeJsRuntime nodeJsRuntime;
  private final ClientModulesProvider modulesProvider;
  private final AtomicReference<Boolean> shutdown = new AtomicReference<>(false);

  public EnginesFactory(Collection<Path> standaloneAnalyzers, Map<String, Path> embeddedPluginsToPath,
    LanguageClientLogOutput globalLogOutput, NodeJsRuntime nodeJsRuntime, ClientModulesProvider modulesProvider) {
    this.standaloneAnalyzers = standaloneAnalyzers;
    this.embeddedPluginsToPath = embeddedPluginsToPath;
    this.logOutput = globalLogOutput;
    this.nodeJsRuntime = nodeJsRuntime;
    this.modulesProvider = modulesProvider;
  }

  public StandaloneSonarLintEngine createStandaloneEngine() {
    if (shutdown.get().equals(true)) {
      throw new IllegalStateException("Language server is shutting down, won't create engine");
    }
    LOG.debug("Starting standalone SonarLint engine...");
    LOG.debug("Using {} analyzers", standaloneAnalyzers.size());

    try {
      var configuration = StandaloneGlobalConfiguration.builder()
        .setSonarLintUserHome(sonarLintUserHomeOverride)
        .addEnabledLanguages(STANDALONE_LANGUAGES)
        .setNodeJs(nodeJsRuntime.getNodeJsPath(), nodeJsRuntime.getNodeJsVersion())
        .addPlugins(standaloneAnalyzers.toArray(Path[]::new))
        .setModulesProvider(modulesProvider)
        .setLogOutput(logOutput)
        .build();

      var engine = newStandaloneEngine(configuration);
      LOG.debug("Standalone SonarLint engine started");
      return engine;
    } catch (Exception e) {
      LOG.error("Error starting standalone SonarLint engine", e);
      throw new IllegalStateException(e);
    }
  }

  StandaloneSonarLintEngine newStandaloneEngine(StandaloneGlobalConfiguration configuration) {
    return new StandaloneSonarLintEngineImpl(configuration);
  }

  public ConnectedSonarLintEngine createConnectedEngine(String connectionId,
    ServerConnectionSettings serverConnectionSettings) {
    if (shutdown.get().equals(true)) {
      throw new IllegalStateException("Language server is shutting down, won't create engine");
    }
    ConnectedGlobalConfiguration.Builder builder;
    if (serverConnectionSettings.isSonarCloudAlias()) {
      builder = ConnectedGlobalConfiguration.sonarCloudBuilder();
    } else {
      builder = ConnectedGlobalConfiguration.sonarQubeBuilder();
    }
    builder
      .setSonarLintUserHome(sonarLintUserHomeOverride)
      .setConnectionId(connectionId)
      .addEnabledLanguages(STANDALONE_LANGUAGES)
      .addEnabledLanguages(CONNECTED_ADDITIONAL_LANGUAGES)
      .enableHotspots()
      .setNodeJs(nodeJsRuntime.getNodeJsPath(), nodeJsRuntime.getNodeJsVersion())
      .setModulesProvider(modulesProvider)
      .setLogOutput(logOutput);

    embeddedPluginsToPath.forEach(builder::useEmbeddedPlugin);

    var engine = newConnectedEngine(builder.build());

    LOG.debug("SonarLint engine started for connection '{}'", connectionId);
    return engine;
  }

  ConnectedSonarLintEngine newConnectedEngine(ConnectedGlobalConfiguration configuration) {
    return new ConnectedSonarLintEngineImpl(configuration);
  }

  public static Set<Language> getStandaloneLanguages() {
    return EnumSet.copyOf(List.of(STANDALONE_LANGUAGES));
  }

  public static Set<Language> getConnectedLanguages() {
    return Set.of(CONNECTED_ADDITIONAL_LANGUAGES);
  }

  public void shutdown() {
    shutdown.set(true);
  }
}
