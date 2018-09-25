/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */

import com.lesfurets.jenkins.unit.global.lib.Library
@Library('hibernate-jenkins-pipeline-helpers@master')
import org.hibernate.jenkins.pipeline.helpers.job.JobHelper
import org.hibernate.jenkins.pipeline.helpers.environment.EnvironmentMap
import org.hibernate.jenkins.pipeline.helpers.version.Version

def execute() {
	JobHelper helper = new JobHelper(this)

	EnvironmentMap environments = new EnvironmentMap([
	        jdk: [
					new StubEnvironment('1', false),
					new StubEnvironment('2', true),
					new StubEnvironment('3', false)
			]
	])

	stage('Configure') {
		def loadedFile
		node() {
			loadedFile = helper.loadYamlConfiguration('job-configuration.yaml')
		}
		helper.configure({
			file loadedFile
			maven {
				defaultTool 'THE Maven default tool'
				producedArtifactPattern "org/hibernate/search/*"
				producedArtifactPattern "org/hibernate/hibernate-search*"
			}
			jdk {
				defaultTool environments.defaults.jdk.name
			}
		})

		properties([
				pipelineTriggers(
						[
								issueCommentTrigger('.*test this please.*'),
								// Normally we don't have snapshot dependencies, so this doesn't matter, but some branches do
								snapshotDependencies()
						]
								+ helper.configuration.tracking.trackedAsString ? [
								// Rebuild when tracked jobs are rebuilt
								upstream(helper.configuration.tracking.trackedAsString)
						]
								: []
				)
		])

		environments.all.each { key, envList ->
			envList.removeAll { itEnv -> itEnv.isDefault() }
		}

		echo "Enabled environments: $environments"

		def releaseVersion = Version.parseReleaseVersion('5.10.2.Final')
		def developmentVersion = Version.parseDevelopmentVersion('5.10.3-SNAPSHOT')

		echo "Release version: $releaseVersion, family: $releaseVersion.family"
		echo "Development version: $developmentVersion, family: $developmentVersion.family"
	}

	stage('Build') {
		node() {
			helper.withMavenWorkspace {
				sh "mvn clean install"
			}
		}
	}
}

class StubEnvironment {
	final String name
	final boolean isDefault

	StubEnvironment(String name, boolean isDefault) {
		this.name = name
		this.isDefault = isDefault
	}

	boolean isDefault() {
		isDefault
	}

	String toString() {
		name
	}
}

return this