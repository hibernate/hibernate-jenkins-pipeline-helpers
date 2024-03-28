/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */

import com.lesfurets.jenkins.unit.global.lib.Library

@Library('hibernate-jenkins-pipeline-helpers@main') _

pipeline {
	stages {
		stage('Configure') {
			steps {
				requireApprovalForPullRequest 'hibernate'
			}
		}
		stage('Build') {
			steps {
				doStuff()
			}
		}
	}
}
