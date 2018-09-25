/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.jenkins.pipeline.helpers.version

import java.util.regex.Pattern

class Version {

	private static final Pattern RELEASE_VERSION_PATTERN = ~/^(\d+)\.(\d+)\.(\d+)\.((?<=\.0\.)(?:Alpha\d+|Beta\d+|CR\d+)|Final)$/

	private static final Pattern DEVELOPMENT_VERSION_PATTERN = ~/^(\d+)\.(\d+)\.(\d+)-SNAPSHOT$/

	static Version parseReleaseVersion(String versionString) {
		def matcher = (versionString =~ RELEASE_VERSION_PATTERN)
		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"Invalid version number: '$versionString'." +
							" Release version numbers must match /$RELEASE_VERSION_PATTERN/."
			)
		}
		return new Version(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), false)
	}

	static Version parseDevelopmentVersion(String versionString) {
		def matcher = (versionString =~ DEVELOPMENT_VERSION_PATTERN)
		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"Invalid version number: '$versionString'." +
							" Development version numbers must match /$DEVELOPMENT_VERSION_PATTERN/."
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
