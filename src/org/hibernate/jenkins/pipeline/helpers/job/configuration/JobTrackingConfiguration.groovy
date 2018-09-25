/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.job.configuration

import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import org.hibernate.jenkins.pipeline.helpers.scm.ScmSource

@PackageScope([PackageScopeTarget.CONSTRUCTORS, PackageScopeTarget.FIELDS, PackageScopeTarget.METHODS])
class JobTrackingConfiguration {
	private final def script
	private final ScmSource scmSource

	private def fileSection = [:]

	private String id
	private String trackedAsString

	JobTrackingConfiguration(def script, ScmSource scmSource) {
		this.script = script
		this.scmSource = scmSource
	}

	public String getId() {
		id
	}

	public String getTrackedAsString() {
		trackedAsString
	}

	public def getFileSection() {
		fileSection
	}

	void complete(def configFile) {
		if (scmSource.branch.tracking) {
			this.id = scmSource.branch.tracking
			this.fileSection = configFile?.tracking?.get(id)
			if (!fileSection) {
				throw new IllegalStateException(
						"Missing tracking configuration for tracking ID '$id'." +
								" Add tracking configuration to the job configuration file." +
								" See the documentation in the Jenkinsfile for more information."
				)
			}

			// Configure jobs that are tracked (should trigger a build of this branch on successful builds)
			// If additional jobs are mentioned in the branch name, take them into account
			// (and prepend "/" to reference jobs outside of the multibranch job)
			trackedAsString = fileSection.tracked?.join(',')

			script.with {
				echo "This is a tracking branch. Tracking ID: $id." +
						" Base branch: ${fileSection.base}." +
						" Tracked Jenkins jobs: $trackedAsString"
			}
		}
	}
}
