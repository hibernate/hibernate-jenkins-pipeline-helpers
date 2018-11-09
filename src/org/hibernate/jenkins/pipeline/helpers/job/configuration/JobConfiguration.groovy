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
import org.hibernate.jenkins.pipeline.helpers.util.DslUtils

@PackageScope([PackageScopeTarget.CONSTRUCTORS, PackageScopeTarget.FIELDS, PackageScopeTarget.METHODS])
class JobConfiguration {
	final def script
	final JdkConfiguration jdk
	final MavenConfiguration maven
	final JobTrackingConfiguration tracking

	private String configurationNodePattern
	private String fileId
	private def file

	public JobConfiguration(def script, ScmSource scmSource) {
		this.script = script
		this.jdk = new JdkConfiguration()
		this.maven = new MavenConfiguration(script)
		this.tracking = new JobTrackingConfiguration(script, scmSource)
	}

	public def getFile() {
		file
	}

	public void setup(@DelegatesTo(DSLElement) Closure closure) {
		DSLElement dslElement = new DSLElement(this)
		DslUtils.delegateTo(dslElement, closure)

		jdk.complete()
		maven.complete()

		JobConfiguration thiz = this
		script.node(configurationNodePattern) {
			// We can't refer to fields directly for some reason, probably because node() messes with the closure
			thiz.file = loadYamlConfiguration(thiz.fileId)
			thiz.script.echo "Job configuration: ${thiz.file}"
		}

		tracking.complete(file)
	}

	private def loadYamlConfiguration(String yamlConfigFileId) {
		try {
			script.configFileProvider([script.configFile(fileId: yamlConfigFileId, variable: "FILE_PATH")]) {
				return script.readYaml(file: script.FILE_PATH)
			}
		}
		catch (Exception e) {
			script.echo "Failed to load configuration file '$yamlConfigFileId'; assuming empty file. Exception was: $e"
			return [:]
		}
	}

	/*
	 * WARNING: this class must be static, because inner classes don't work well in Jenkins.
	 * "Qualified this" in particular doesn't work.
	 */
	@PackageScope([PackageScopeTarget.CONSTRUCTORS])
	public static class DSLElement {
		private final JobConfiguration configuration

		private DSLElement(JobConfiguration configuration) {
			this.configuration = configuration
		}

		void file(String fileId) {
			configuration.fileId = fileId
		}

		void configurationNodePattern(String nodePattern) {
			configuration.configurationNodePattern = nodePattern
		}

		void jdk(@DelegatesTo(JdkConfiguration.DSLElement) Closure closure) {
			DslUtils.delegateTo(configuration.jdk.dsl(), closure)
		}

		void maven(@DelegatesTo(MavenConfiguration.DSLElement) Closure closure) {
			DslUtils.delegateTo(configuration.maven.dsl(), closure)
		}
	}
}
