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
		Closure copy = closure.rehydrate(dslElement, closure.owner, closure.thisObject)
		copy.resolveStrategy = resolveStrategy
		copy()
	}

}
