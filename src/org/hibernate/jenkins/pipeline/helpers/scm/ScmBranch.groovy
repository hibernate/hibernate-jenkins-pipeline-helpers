/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.scm

import java.util.regex.Pattern

class ScmBranch {
	private static final Pattern PRIMARY_PATTERN = ~/^master|\d+\.\d+$/
	private static final Pattern TRACKING_PATTERN = ~/^tracking-(.+)$/

	String name

	@Override
	String toString() {
		"ScmBranch(name:$name)"
	}

	/**
	 * @return Whether this branch is "primary", i.e. it's either "master" or a maintenance branch.
	 * The purpose of primary branches is to hold the history of a major version of the code,
	 * whereas the only purpose  of "feature" branches is to eventually be merged into a primary branch.
	 */
	boolean isPrimary() {
		(name ==~ PRIMARY_PATTERN)
	}

	/**
	 * @return The name of the "tracking" for this branch, to allow for configuration retrieval.
	 */
	String getTracking() {
		def matcher = (name =~ TRACKING_PATTERN)
		return matcher.matches() ? matcher.group(1) : null
	}
}