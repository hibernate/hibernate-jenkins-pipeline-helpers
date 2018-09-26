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
	static void delegateTo(def dslElement, Closure closure) {
		/*
		 * WARNING: In Jenkins, using closure.rehydrate(...) will not work.
		 * The resulting copy of the closure will execute, but any of its calls to the delegate
		 * will be silently ignored.
		 * Thus we just take the dirty path and mutate the original closure.
		 */
		closure.delegate = dslElement
		/*
		 * WARNING: In Jenkins, only the default "resolveStrategy" can be expected to work correctly,
		 * because other strategies rely on reflection (GroovyObject.invoke(String, Map)
		 * or GroovyObject.getProperty(String)), which are not safe to execute in the Jenkins sandbox.
		 * Thus we don't call closure.resolveStrategy = ... here.
		 */
		closure()
	}

}
