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
		localRepositoryRelativePath = "maven-repository"
	}

	public String getLocalRepositoryPath() {
		String workspaceTempPath = script.pwd(tmp: true)
		if (!workspaceTempPath) {
			throw new IllegalStateException(
					"Cannot determine the Maven local repository path if the Jenkins temporary workspace path is unknown." +
					" Try again within a node() step."
			)
		}
		return "$workspaceTempPath/$localRepositoryRelativePath"
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
		return new DSLElement(this)
	}

	/*
	 * WARNING: this class must be static, because inner classes don't work well in Jenkins.
	 * "Qualified this" in particular doesn't work.
	 */
	@PackageScope([PackageScopeTarget.CONSTRUCTORS])
	public static class DSLElement {
		private final MavenConfiguration configuration

		DSLElement(MavenConfiguration configuration) {
			this.configuration = configuration
		}

		void defaultTool(String toolName) {
			configuration.defaultTool = toolName
		}

		void producedArtifactPattern(String pattern) {
			configuration.producedArtifactPatterns.add(pattern)
		}
	}
}
