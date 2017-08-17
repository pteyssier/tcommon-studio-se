// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.librariesmanager.maven;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.model.general.ModuleNeeded.ELibraryInstallStatus;
import org.talend.core.model.general.ModuleStatusProvider;
import org.talend.core.nexus.IRepositoryArtifactHandler;
import org.talend.core.nexus.NexusServerBean;
import org.talend.core.nexus.RepositoryArtifactHandlerManager;
import org.talend.core.nexus.TalendLibsServerManager;
import org.talend.core.nexus.TalendMavenResolver;
import org.talend.core.runtime.maven.MavenArtifact;
import org.talend.core.runtime.maven.MavenUrlHelper;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.utils.PomUtil;
import org.talend.utils.io.FilesUtils;

/**
 * created by wchen on 2015-5-14 Detailled comment
 *
 */
public class MavenArtifactsHandler {

    public MavenArtifactsHandler() {
    }

    public void install(Map<String, String> jarSourceAndMavenUri, boolean deploy) throws Exception {
        for (String mavenUri : jarSourceAndMavenUri.keySet()) {
            try {
                install(mavenUri, jarSourceAndMavenUri.get(mavenUri), null, deploy);
            } catch (Exception e) {
                ExceptionHandler.process(e);
                continue;
            }
        }
    }

    public void install(String path, String mavenUri, boolean deploy) throws Exception {
        install(mavenUri, path, null, deploy);
    }

    /**
     * 
     * DOC Talend Comment method "deployToLocalMaven".
     * 
     * @param uriOrPath can be a filePath or platform uri
     * @param mavenUri maven uri
     * @throws Exception
     */
    public void install(String path, String mavenUri) throws Exception {
        install(path, mavenUri, true);
    }

    /**
     * Deploy the lib with fixed pom file. if not set, will generate a default one.
     */
    public void install(String mavenUri, String libPath, String pomPath, boolean deploy) throws Exception {
        MavenArtifact parseMvnUrl = MavenUrlHelper.parseMvnUrl(mavenUri);
        if (parseMvnUrl != null && libPath != null && libPath.length() > 0) {
            File libFile = new File(libPath);
            if (!libFile.exists()) {
                return;
            }
            // check if already exist in nexus server with this mvnUri
            if (!parseMvnUrl.getVersion().endsWith(MavenUrlHelper.VERSION_SNAPSHOT)) {
                NexusServerBean customNexusServer = TalendLibsServerManager.getInstance().getCustomNexusServer();
                IRepositoryArtifactHandler hander = RepositoryArtifactHandlerManager.getRepositoryHandler(customNexusServer);
                if (hander != null) {
                    List<MavenArtifact> search = hander.search(parseMvnUrl.getGroupId(), parseMvnUrl.getArtifactId(),
                            parseMvnUrl.getVersion(), true, false);
                    if (search.size() == 1 && !"pom".equals(search.get(0).getType())) {
                        throw new Exception("Artifact already deployed with maven uri" + mavenUri
                                + " , please try to use custom maven uri to deploy agian! ");
                    }
                }
            }

            // lib
            String artifactType = parseMvnUrl.getType();
            if (artifactType == null || "".equals(artifactType)) {
                artifactType = TalendMavenConstants.PACKAGING_JAR;
            }
            TalendMavenResolver.upload(parseMvnUrl.getGroupId(), parseMvnUrl.getArtifactId(), parseMvnUrl.getClassifier(),
                    artifactType, parseMvnUrl.getVersion(), libFile);

            ModuleStatusProvider.getDeployStatusMap().put(mavenUri, ELibraryInstallStatus.DEPLOYED);
            ModuleStatusProvider.getStatusMap().put(mavenUri, ELibraryInstallStatus.INSTALLED);

            // pom
            boolean generated = false;
            File pomFile = null;
            if (pomPath != null && pomPath.length() > 0) {
                pomFile = new File(pomPath);
                if (!pomFile.exists()) {
                    pomFile = null;
                }
            }
            if (pomFile == null) {
                pomFile = new File(PomUtil.generatePom2(parseMvnUrl));
                generated = true;
            }

            String pomType = TalendMavenConstants.PACKAGING_POM;
            if (pomFile != null && pomFile.exists()) {
                TalendMavenResolver.upload(parseMvnUrl.getGroupId(), parseMvnUrl.getArtifactId(), parseMvnUrl.getClassifier(),
                        pomType, parseMvnUrl.getVersion(), pomFile);
            }

            if (deploy) {
                deploy(libFile, parseMvnUrl);
            }
            if (generated) { // only for generate pom
                FilesUtils.deleteFolder(pomFile.getParentFile(), true);
            }
        }
    }

    public void deploy(File content, MavenArtifact artifact) throws Exception {
        NexusServerBean customNexusServer = TalendLibsServerManager.getInstance().getCustomNexusServer();
        IRepositoryArtifactHandler hander = RepositoryArtifactHandlerManager.getRepositoryHandler(customNexusServer);
        if (hander != null) {
            hander.deploy(content, artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(),
                    artifact.getVersion());
        }

    }

}
