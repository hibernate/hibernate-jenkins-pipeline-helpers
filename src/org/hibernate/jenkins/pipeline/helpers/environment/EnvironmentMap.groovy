/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.environment

class EnvironmentMap {
	@Delegate
	private final Map<String, List<?>> all

	final Map<String, ?> defaults

	EnvironmentMap(Map<String, List<?>> all) {
		this(all, {it -> it.isDefault()})
	}

	EnvironmentMap(Map<String, List<?>> all, Closure defaultPicker) {
		this.all = all
		// Defaults are set before any user modification (environment removals); this is on purpose.
		all.each { key, envList ->
			defaults.put(key, envList.find(defaultPicker))
		}
	}

	@Override
	String toString() {
		/*
		 * There is something deeply wrong with collections in pipelines:
		 * - referencing a collection directly with interpolation ("$foo") sometimes results
		 *   in the echo statement not being executed at all, without even an exception
		 * - calling collection.toString() only prints the first element in the collection
		 * - calling flatten().join(', ') only prints the first element
		 * Thus the workaround below...
		 */
		String result = ''
		all.values().each { envList ->
			envList.each {
				if (result) {
					result += ', '
				}
				else {
					result = ''
				}
				result += it.toString()
			}
		}
		return result
	}

	boolean isEmpty() {
		all.isEmpty() || all.every { key, envList -> envList.isEmpty() }
	}
}
