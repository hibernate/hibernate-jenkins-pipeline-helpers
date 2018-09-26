/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.alternative

class AlternativeMultiMap<T> {

	static <T> AlternativeMultiMap<T> create(Map<String, ? extends Collection<? extends T>> map) {
		return create(map, { it -> it.isDefault() })
	}

	static <T> AlternativeMultiMap create(Map<String, ? extends Collection<? extends T>> map, Closure defaultPicker) {
		return new AlternativeMultiMap(
				map.collectEntries { key, collection -> [key, AlternativeSet.create(collection, defaultPicker)] }
		)
	}

	final Map<String, AlternativeSet<? extends T>> content

	private AlternativeMultiMap(Map<String, AlternativeSet<? extends T>> content) {
		this.content = content
	}

	boolean isAnyEnabled() {
		content.any { key, set -> set.enabled }
	}

	String getEnabledAsString() {
		/*
		 * There is something deeply wrong with collections in pipelines:
		 * - referencing a collection directly with interpolation ("$foo") sometimes results
		 *   in the echo statement not being executed at all, without even an exception
		 * - calling collection.toString() only prints the first element in the collection
		 * - calling flatten().join(', ') only prints the first element
		 * Thus the workaround below...
		 */
		String result = ''
		content.values().each { set ->
			set.enabled.each {
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
}
