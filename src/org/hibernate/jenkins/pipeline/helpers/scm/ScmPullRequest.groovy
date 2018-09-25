/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.scm

class ScmPullRequest {
	String id
	ScmBranch source
	ScmBranch target

	@Override
	String toString() {
		"ScmPullRequest(id: $id, source: $source, target: $target)"
	}
}