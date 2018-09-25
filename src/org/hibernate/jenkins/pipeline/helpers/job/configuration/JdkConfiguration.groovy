/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.job.configuration

import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import groovy.transform.TypeChecked

@TypeChecked
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

	@TypeChecked
	@PackageScope([PackageScopeTarget.CONSTRUCTORS])
	public class DSLElement {
		DSLElement() {
		}

		void defaultTool(String toolName) {
			defaultTool = toolName
		}
	}
}
