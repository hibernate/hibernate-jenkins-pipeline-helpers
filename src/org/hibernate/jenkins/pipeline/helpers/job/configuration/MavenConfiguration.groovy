/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.job.configuration

import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget

@PackageScope([PackageScopeTarget.CONSTRUCTORS, PackageScopeTarget.FIELDS, PackageScopeTarget.METHODS])
class MavenConfiguration {
	private final def script

	private String localRepositoryRelativePath
	private String defaultTool
	private List<String> producedArtifactPatterns = []

	MavenConfiguration(def script) {
		this.script = script
		localRepositoryRelativePath = ".repository"
	}

	public String getLocalRepositoryPath() {
		String workspacePath = script.env.WORKSPACE
		if (!workspacePath) {
			throw new IllegalStateException(
					"Cannot determine the Maven local repository path if the Jenkins workspace path is unknown." +
					" Try again within a node() step."
			)
		}
		return "$workspacePath/$localRepositoryRelativePath"
	}

	public String getDefaultTool() {
		defaultTool
	}

	public getProducedArtifactPatterns() {
		return producedArtifactPatterns
	}

	void complete() {
		if (!defaultTool) {
			throw new IllegalStateException("Missing default tool for Maven")
		}
	}

	// Workaround for https://issues.jenkins-ci.org/browse/JENKINS-41896 (which apparently also affects non-static classes)
	DSLElement dsl() {
		return new DSLElement()
	}

	@PackageScope([PackageScopeTarget.CONSTRUCTORS])
	public class DSLElement {
		DSLElement() {
		}

		void defaultTool(String toolName) {
			defaultTool = toolName
		}

		void producedArtifactPattern(String pattern) {
			producedArtifactPatterns.add(pattern)
		}
	}
}
