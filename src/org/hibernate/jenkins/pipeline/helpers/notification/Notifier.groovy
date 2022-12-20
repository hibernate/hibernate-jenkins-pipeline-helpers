/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.notification

import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget

class Notifier {
	private final def script

	Notifier(def script) {
		this.script = script
	}

	void doNotifyBuildResult(Map args) {
		Configuration configuration = new Configuration(args)
		boolean success = script.currentBuild.result == 'SUCCESS'
		boolean successAfterSuccess = success &&
				script.currentBuild.previousBuild != null && script.currentBuild.previousBuild.result == 'SUCCESS'

		def recipientProviders = []
		String explicitRecipients = null

		// Always notify people who explicitly requested a build.
		recipientProviders.add(script.requestor())

		// Don't notify anyone else for a "success after a success"... unless asked to.
		if (!successAfterSuccess || configuration.notifySuccessAfterSuccess) {
			if (script.env.CHANGE_ID) {
				// PR build: notify the authors of the changeset.
				recipientProviders.add(script.developers())
				recipientProviders.add(script.culprits())
			}
			else {
				// Non-PR builds: notify maintainers.
				explicitRecipients = configuration.maintainers
			}
		}

		// See https://plugins.jenkins.io/email-ext#Email-extplugin-PipelineExamples
		script.emailext(
				subject: '${DEFAULT_SUBJECT}',
				body: '${DEFAULT_CONTENT}',
				recipientProviders: recipientProviders,
				to: explicitRecipients
		)
	}

	/*
	 * WARNING: this class must be static, because inner classes don't work well in Jenkins.
	 * "Qualified this" in particular doesn't work.
	 */
	@PackageScope([PackageScopeTarget.CONSTRUCTORS])
	public static class Configuration {
		/**
		 * Space-separated emails of maintainers to notify of build results on primary branches.
		 */
		String maintainers
		/**
		 * Whether maintainers should always be notified.
		 */
		boolean notifySuccessAfterSuccess = false
	}
}
