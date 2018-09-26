/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.util

import groovy.transform.TypeChecked

@TypeChecked
class DslUtils {

	private DslUtils() {
	}

	// See http://groovy-lang.org/dsls.html#section-delegatesto
	static void delegateTo(int resolveStrategy, def dslElement, Closure closure) {
		/*
		 * WARNING: In Jenkins, using closure.rehydrate(...) will not work.
		 * The resulting copy of the closure will execute, but any of its calls to the delegate
		 * will be silently ignored.
		 * Thus we just take the dirty path and mutate the original closure.
		 */
		closure.delegate = dslElement
		closure.resolveStrategy = resolveStrategy
		closure()
	}

}
