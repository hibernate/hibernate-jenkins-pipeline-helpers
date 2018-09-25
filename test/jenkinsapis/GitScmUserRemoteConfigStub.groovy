/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package jenkinsapis


class GitScmUserRemoteConfigStub {

	private final String url

	GitScmUserRemoteConfigStub(String url) {
		this.url = url
	}

	@Override
	String toString() {
		"${getClass().getSimpleName()}(url=$url)"
	}

	String getUrl() {
		url
	}

}