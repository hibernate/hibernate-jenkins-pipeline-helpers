/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package jenkinsapis

class EnvStub {

	String BRANCH_NAME = "branch_name"
	String WORKSPACE = "/path/to/workspace"
	String CHANGE_ID = null
	String CHANGE_TARGET = null
	String CHANGE_BRANCH = null

	@Override
	String toString() { getClass().getSimpleName() }

}
