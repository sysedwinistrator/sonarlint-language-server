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

import com.google.gson.annotations.Expose;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageClient;
import org.sonarsource.sonarlint.core.clientapi.backend.rules.ActiveRuleParamDto;
import org.sonarsource.sonarlint.core.clientapi.client.SuggestBindingParams;
import org.sonarsource.sonarlint.core.clientapi.client.fs.FindFileByNamesInScopeResponse;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.RuleType;
import org.sonarsource.sonarlint.core.serverapi.hotspot.ServerHotspotDetails;
import org.sonarsource.sonarlint.ls.commands.ShowAllLocationsCommand;

public interface SonarLintExtendedLanguageClient extends LanguageClient {

  @JsonNotification("sonarlint/suggestBinding")
  void suggestBinding(SuggestBindingParams binding);

  @JsonRequest("sonarlint/findFileByNamesInFolder")
  CompletableFuture<FindFileByNamesInScopeResponse> findFileByNamesInFolder(FindFileByNamesInFolder params);

  @JsonNotification("sonarlint/showSonarLintOutput")
  void showSonarLintOutput();

  @JsonNotification("sonarlint/openJavaHomeSettings")
  void openJavaHomeSettings();

  @JsonNotification("sonarlint/openPathToNodeSettings")
  void openPathToNodeSettings();

  @JsonNotification("sonarlint/openConnectionSettings")
  void openConnectionSettings(boolean isSonarCloud);

  @JsonNotification("sonarlint/showRuleDescription")
  void showRuleDescription(ShowRuleDescriptionParams params);

  @JsonNotification("sonarlint/showHotspot")
  void showHotspot(ServerHotspotDetails hotspot);

  @JsonNotification("sonarlint/showIssueOrHotspot")
  void showIssueOrHotspot(ShowAllLocationsCommand.Param params);

  @JsonNotification("sonarlint/submitToken")
  void submitToken(String token);

  @JsonRequest("sonarlint/isIgnoredByScm")
  CompletableFuture<Boolean> isIgnoredByScm(String fileUri);

  @JsonRequest("sonarlint/isOpenInEditor")
  CompletableFuture<Boolean> isOpenInEditor(String fileUri);

  @JsonNotification("sonarlint/showNotificationForFirstSecretsIssue")
  void showFirstSecretDetectionNotification();

  class ShowRuleDescriptionParams {

    private static final String TAINT_RULE_REPO_SUFFIX = "security";
    @Expose
    private final String key;
    @Expose
    private final String name;
    @Expose
    private final String htmlDescription;
    @Expose
    private final RuleDescriptionTab[] htmlDescriptionTabs;
    @Expose
    private final String type;
    @Expose
    private final String severity;
    @Expose
    private final boolean isTaint;
    @Expose
    private final RuleParameter[] parameters;

    public ShowRuleDescriptionParams(String ruleKey, String ruleName, String htmlDescription, RuleDescriptionTab[] htmlDescriptionTabs,
      RuleType type,  IssueSeverity severity, Collection<ActiveRuleParamDto> params) {
      this.key = ruleKey;
      this.name = ruleName;
      this.htmlDescription = htmlDescription;
      this.htmlDescriptionTabs = htmlDescriptionTabs;
      this.type = type.toString();
      this.severity = severity.toString();
      this.isTaint = ruleKey.contains(TAINT_RULE_REPO_SUFFIX) && type == RuleType.VULNERABILITY;
      this.parameters = params.stream().map(p -> new RuleParameter(p.getName(), p.getDescription(), p.getDefaultValue())).toArray(RuleParameter[]::new);
    }

    public String getKey() {
      return key;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public String getSeverity() {
      return severity;
    }

    public boolean isTaint() {
      return isTaint;
    }

    public RuleParameter[] getParameters() {
      return parameters;
    }

    public String getHtmlDescription() {
      return htmlDescription;
    }

    public RuleDescriptionTab[] getHtmlDescriptionTabs() {
      return htmlDescriptionTabs;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ShowRuleDescriptionParams that = (ShowRuleDescriptionParams) o;
      return isTaint == that.isTaint
        && Objects.equals(key, that.key)
        && Objects.equals(name, that.name)
        && Objects.equals(htmlDescription, that.htmlDescription)
        && Arrays.equals(htmlDescriptionTabs, that.htmlDescriptionTabs)
        && Objects.equals(type, that.type) && Objects.equals(severity, that.severity)
        && Arrays.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(key, name, htmlDescription, type, severity, isTaint);
      result = 31 * result + Arrays.hashCode(htmlDescriptionTabs);
      result = 31 * result + Arrays.hashCode(parameters);
      return result;
    }
  }

  class RuleDescriptionTab {

    @Expose
    final String title;
    @Expose
    final String description;

    public RuleDescriptionTab(String title, String description) {
      this.title = title;
      this.description = description;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RuleDescriptionTab that = (RuleDescriptionTab) o;
      return Objects.equals(title, that.title) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
      return Objects.hash(title, description);
    }
  }

  class FindFileByNamesInFolder {
    @Expose
    private final String folderUri;
    @Expose
    private final String[] filenames;

    public FindFileByNamesInFolder(String folderUri, List<String> filenames) {
      this.folderUri = folderUri;
      this.filenames = filenames.toArray(new String[0]);
    }

    public String getFolderUri() {
      return folderUri;
    }

    public String[] getFilenames() {
      return filenames;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FindFileByNamesInFolder that = (FindFileByNamesInFolder) o;
      return Objects.equals(folderUri, that.folderUri) && Arrays.equals(filenames, that.filenames);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(folderUri);
      result = 31 * result + Arrays.hashCode(filenames);
      return result;
    }
  }

  class RuleParameter {
    @Expose
    final String name;
    @Expose
    final String description;
    @Expose
    final String defaultValue;

    public RuleParameter(String name, @Nullable String description, @Nullable String defaultValue) {
      this.name = name;
      this.description = description;
      this.defaultValue = defaultValue;
    }

    public String getName() {
      return name;
    }

    @CheckForNull
    public String getDescription() {
      return description;
    }

    @CheckForNull
    public String getDefaultValue() {
      return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      var that = (RuleParameter) o;
      return name.equals(that.name) &&
        Objects.equals(description, that.description) &&
        Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, description, defaultValue);
    }
  }

  /**
   * Fetch java configuration for a given file.
   * See: https://github.com/redhat-developer/vscode-java/commit/e29f6df2db016c514afd8d2b69462ad2ef1de867
   */
  @JsonRequest("sonarlint/getJavaConfig")
  CompletableFuture<GetJavaConfigResponse> getJavaConfig(String fileUri);

  class GetJavaConfigResponse {

    private String projectRoot;
    private String sourceLevel;
    private String[] classpath;
    private boolean isTest;
    private String vmLocation;

    public String getProjectRoot() {
      return projectRoot;
    }

    public void setProjectRoot(String projectRoot) {
      this.projectRoot = projectRoot;
    }

    public String getSourceLevel() {
      return sourceLevel;
    }

    public void setSourceLevel(String sourceLevel) {
      this.sourceLevel = sourceLevel;
    }

    public String[] getClasspath() {
      return classpath;
    }

    public void setClasspath(String[] classpath) {
      this.classpath = classpath;
    }

    public boolean isTest() {
      return isTest;
    }

    public void setTest(boolean isTest) {
      this.isTest = isTest;
    }

    @CheckForNull
    public String getVmLocation() {
      return vmLocation;
    }

    public void setVmLocation(String vmLocation) {
      this.vmLocation = vmLocation;
    }

  }

  @JsonNotification("sonarlint/browseTo")
  void browseTo(String link);

  @JsonRequest("sonarlint/getBranchNameForFolder")
  CompletableFuture<String> getBranchNameForFolder(String folderUri);

  class ReferenceBranchForFolder {
    private final String folderUri;
    @Nullable
    private final String branchName;

    private ReferenceBranchForFolder(String folderUri, @Nullable String branchName) {
      this.folderUri = folderUri;
      this.branchName = branchName;
    }

    public String getFolderUri() {
      return folderUri;
    }

    @CheckForNull
    public String getBranchName() {
      return branchName;
    }

    public static ReferenceBranchForFolder of(String folderUri, @Nullable String branchName) {
      return new ReferenceBranchForFolder(folderUri, branchName);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      var that = (ReferenceBranchForFolder) o;
      return folderUri.equals(that.folderUri) && Objects.equals(branchName, that.branchName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(folderUri, branchName);
    }
  }

  @JsonNotification("sonarlint/setReferenceBranchNameForFolder")
  void setReferenceBranchNameForFolder(ReferenceBranchForFolder newReferenceBranch);

  @JsonNotification("sonarlint/needCompilationDatabase")
  void needCompilationDatabase();

  @JsonNotification("sonarlint/reportConnectionCheckResult")
  void reportConnectionCheckResult(ConnectionCheckResult result);

  class ConnectionCheckResult {
    private final String connectionId;
    private final boolean success;
    @Nullable
    private final String reason;

    private ConnectionCheckResult(String connectionId, boolean success, @Nullable String reason) {
      this.connectionId = connectionId;
      this.success = success;
      this.reason = reason;
    }
    public static ConnectionCheckResult success(String connectionId) {
      return new ConnectionCheckResult(connectionId, true, null);
    }

    public static ConnectionCheckResult failure(String connectionId, String reason) {
      return new ConnectionCheckResult(connectionId, false, reason);
    }

    public String getConnectionId() {
      return connectionId;
    }

    public boolean isSuccess() {
      return success;
    }

    @CheckForNull
    public String getReason() {
      return reason;
    }
  }

  @JsonRequest("sonarlint/getTokenForServer")
  CompletableFuture<String> getTokenForServer(String serverUrlOrOrganization);

  @JsonNotification("sonarlint/publishSecurityHotspots")
  void publishSecurityHotspots(PublishDiagnosticsParams publishDiagnosticsParams);

  @JsonNotification("sonarlint/readyForTests")
  void readyForTests();
}
