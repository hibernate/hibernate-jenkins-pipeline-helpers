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
class JdkConfiguration {
	private String defaultTool

	JdkConfiguration() {
	}

	public String getDefaultTool() {
		defaultTool
	}

	void complete() {
		if (!defaultTool) {
			throw new IllegalStateException("Missing default tool for JDK")
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
		private final JdkConfiguration configuration

		private DSLElement(JdkConfiguration configuration) {
			this.configuration = configuration
		}

		void defaultTool(String toolName) {
			configuration.defaultTool = toolName
		}
	}
}
