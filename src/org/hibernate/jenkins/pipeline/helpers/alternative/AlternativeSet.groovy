/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.alternative

class AlternativeSet<T> {

	static <T> AlternativeSet<T> create(Collection<T> all, Closure defaultPicker) {
		List<T> enabled = []
		enabled.addAll(all)

		// Select the default
		T defaultAlternative = enabled.find(defaultPicker)

		return new AlternativeSet(enabled, defaultAlternative)
	}

	// Sets are buggy in Jenkins (removeAll doesn't work in particular), so we'll use List...
	final List<T> enabled
	private final T default_

	private AlternativeSet(List<T> enabled, T default_) {
		this.enabled = enabled
		this.default_ = default_
	}

	@Override
	String toString() {
		return "${getClass().simpleName}(default: $default_, enabled: $enabled)"
	}

	T getDefault() {
		default_
	}
}
