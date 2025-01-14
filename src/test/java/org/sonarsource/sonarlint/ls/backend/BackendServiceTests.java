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
package org.sonarsource.sonarlint.ls.backend;

import java.util.Optional;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.clientapi.SonarLintBackend;
import org.sonarsource.sonarlint.core.clientapi.backend.config.ConfigurationService;
import org.sonarsource.sonarlint.core.clientapi.backend.config.binding.DidUpdateBindingParams;
import org.sonarsource.sonarlint.core.clientapi.backend.hotspot.HotspotService;
import org.sonarsource.sonarlint.core.clientapi.backend.hotspot.OpenHotspotInBrowserParams;
import org.sonarsource.sonarlint.core.serverconnection.ProjectBinding;
import org.sonarsource.sonarlint.ls.connected.ProjectBindingWrapper;
import org.sonarsource.sonarlint.ls.connected.ServerIssueTrackerWrapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackendServiceTests {

  SonarLintBackend backend = mock(SonarLintBackend.class);
  HotspotService hotspotService = mock(HotspotService.class);
  ConfigurationService configurationService = mock(ConfigurationService.class);
  BackendService underTest = new BackendService(backend);

  @BeforeEach
  void init() {
    when(backend.getHotspotService()).thenReturn(hotspotService);
    when(backend.getConfigurationService()).thenReturn(configurationService);
  }

  @Test
  void openHotspotInBrowser() {
    underTest.openHotspotInBrowser(mock(OpenHotspotInBrowserParams.class));

    verify(backend).getHotspotService();
    verify(hotspotService).openHotspotInBrowser(any());
  }

  @Test
  void updateBinding() {
    underTest.updateBinding(mock(DidUpdateBindingParams.class));

    verify(backend).getConfigurationService();
    verify(configurationService).didUpdateBinding(any());
  }

  @Test
  void getConfigScopeDtoWithBinding() {
    var workspaceUri = "/workspace";
    var connectionId = "connectionId";
    var binding = mock(ProjectBinding.class);
    var bindingWrapper = new ProjectBindingWrapper(connectionId, binding, mock(ConnectedSonarLintEngine.class), mock(ServerIssueTrackerWrapper.class));
    var result = underTest.getConfigScopeDto(new WorkspaceFolder(workspaceUri), Optional.of(bindingWrapper));

    assertThat(result.getId()).isEqualTo(workspaceUri);
    assertThat(result.getParentId()).isNull();
    assertThat(result.getBinding().getConnectionId()).isEqualTo(connectionId);
  }

  @Test
  void getConfigScopeDtoWithoutBinding() {
    var workspaceUri = "/workspace";
    var result = underTest.getConfigScopeDto(new WorkspaceFolder(workspaceUri), Optional.empty());

    assertThat(result.getId()).isEqualTo(workspaceUri);
    assertThat(result.getParentId()).isNull();
    assertThat(result.getBinding().getConnectionId()).isNull();
    assertThat(result.getBinding().getSonarProjectKey()).isNull();
    assertThat(result.getBinding().isBindingSuggestionDisabled()).isFalse();
  }


}
