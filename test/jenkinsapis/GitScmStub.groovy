/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package jenkinsapis

class GitScmStub {

	@Override
	String toString() { getClass().getSimpleName() }

	List<GitScmUserRemoteConfigStub> getUserRemoteConfigs() {
		return [
		        new GitScmUserRemoteConfigStub("git@github.com:hibernate/hibernate-search.git"),
				new GitScmUserRemoteConfigStub("git@github.com:hibernate/hibernate-search.git")
		]
	}

}
