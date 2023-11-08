/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.version

import java.util.regex.Pattern

class Version {

	enum Scheme {
		JBOSS_CLASSIC(~/^(\d+)\.(\d+)\.(\d+)\.((?<=\.0\.)(?:Alpha\d+|Beta\d+|CR\d+)|Final)$/, ~/^(\d+)\.(\d+)\.(\d+)-SNAPSHOT$/),
		JBOSS_NO_FINAL(~/^(\d+)\.(\d+)\.(\d+)(?:\.((?<=\.0\.)(?:Alpha\d+|Beta\d+|CR\d+)))?$/, ~/^(\d+)\.(\d+)\.(\d+)-SNAPSHOT$/);
		final Pattern releaseVersionPattern
		final Pattern developmentVersionPattern
		private Scheme(Pattern releaseVersionPattern, Pattern developmentVersionPattern) {
			this.releaseVersionPattern = releaseVersionPattern
			this.developmentVersionPattern = developmentVersionPattern
		}
	}

	static Version parseReleaseVersion(String versionString, Scheme scheme = Scheme.JBOSS_CLASSIC) {
		def matcher = (versionString =~ scheme.releaseVersionPattern)
		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"Invalid version number: '$versionString'." +
							" Release version numbers must match /$scheme.releaseVersionPattern/."
			)
		}
		return new Version(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), false)
	}

	static Version parseDevelopmentVersion(String versionString, Scheme scheme = Scheme.JBOSS_CLASSIC) {
		def matcher = (versionString =~ scheme.developmentVersionPattern)
		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"Invalid version number: '$versionString'." +
							" Development version numbers must match /$scheme.developmentVersionPattern/."
			)
		}

		return new Version(matcher.group(1), matcher.group(2), matcher.group(3), null, true)
	}

	final String major
	final String minor
	final String micro
	final String qualifier
	final boolean snapshot

	Version(String major, String minor, String micro, String qualifier, boolean snapshot) {
		this.major = major
		this.minor = minor
		this.micro = micro
		this.qualifier = qualifier
		this.snapshot = snapshot
	}

	@Override
	String toString() {
		[major, minor, micro, qualifier].findAll({ it != null }).join('.') + (snapshot ? '-SNAPSHOT' : '')
	}

	String getFamily() {
		"$major.$minor"
	}
}
