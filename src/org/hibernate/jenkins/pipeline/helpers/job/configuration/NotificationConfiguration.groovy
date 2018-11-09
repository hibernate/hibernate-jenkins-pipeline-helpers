/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.job.configuration

import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import org.jenkinsci.plugins.credentialsbinding.impl.CredentialNotFoundException

@PackageScope([PackageScopeTarget.CONSTRUCTORS, PackageScopeTarget.FIELDS, PackageScopeTarget.METHODS])
class NotificationConfiguration {
	private final def script

	private final List endpoints = []
	private final List<String> gitterEndpointsCredentialsIds = []

	NotificationConfiguration(def script) {
		this.script = script
	}

	public List getEndpoints() {
		endpoints
	}

	void complete(def configFile) {
		if (configFile?.notification?.gitter) {
			def gitterConfig = configFile.notification.gitter
			if (gitterConfig.urlCredentialsId instanceof String) {
				gitterEndpointsCredentialsIds.add(gitterConfig.urlCredentialsId)
			}
			else if (gitterConfig.urlCredentialsId instanceof List) {
				gitterConfig.urlCredentialsId.each { credentialsId ->
					gitterEndpointsCredentialsIds.add(credentialsId)
				}
			}
		}
		
		gitterEndpointsCredentialsIds.each { credentialsId ->
			this.endpoints.add([
					urlInfo: [urlType: 'SECRET', urlOrId: credentialsId, buildNotes: '']
			])
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
		private final NotificationConfiguration configuration

		private DSLElement(NotificationConfiguration configuration) {
			this.configuration = configuration
		}

		void endpoint(def endpoint) {
			configuration.endpoints.add( endpoint )
		}

		void gitterEndpoint(String credentialsId) {
			configuration.gitterEndpointsCredentialsIds.add( credentialsId )
		}
	}
}
