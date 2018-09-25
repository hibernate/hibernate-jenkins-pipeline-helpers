/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */

import org.hibernate.jenkins.pipeline.helpers.version.Version
import org.junit.Assert
import org.junit.Test

class VersionTest {

	@Test
	void release_valid() {
		testReleaseVersion('5.10.5.Final', '5', '10', '5', 'Final')
		testReleaseVersion('6.0.0.Final', '6', '0', '0', 'Final')
		testReleaseVersion('6.0.0.Alpha1', '6', '0', '0', 'Alpha1')
		testReleaseVersion('6.0.0.Alpha12', '6', '0', '0', 'Alpha12')
		testReleaseVersion('6.0.0.Beta1', '6', '0', '0', 'Beta1')
		testReleaseVersion('6.0.0.Beta12', '6', '0', '0', 'Beta12')
		testReleaseVersion('6.0.0.CR1', '6', '0', '0', 'CR1')
		testReleaseVersion('6.0.0.CR12', '6', '0', '0', 'CR12')
	}

	@Test
	void development_valid() {
		testDevelopmentVersion('5.10.5-SNAPSHOT', '5', '10', '5', null)
		testDevelopmentVersion('6.0.0-SNAPSHOT', '6', '0', '0', null)
	}

	@Test(expected = IllegalArgumentException)
	void release_snapshot() {
		Version.parseReleaseVersion('7.15.5-SNAPSHOT')
	}

	@Test(expected = IllegalArgumentException)
	void release_missingMicro() {
		Version.parseReleaseVersion('7.15.Final')
	}

	@Test(expected = IllegalArgumentException)
	void release_missingMinorAndMicro() {
		Version.parseReleaseVersion('7.Final')
	}

	@Test(expected = IllegalArgumentException)
	void release_unknownQualifier() {
		Version.parseReleaseVersion('7.15.5.Foo')
	}

	@Test(expected = IllegalArgumentException)
	void release_finalQualifierWithNumber() {
		Version.parseReleaseVersion('7.15.5.Final2')
	}

	@Test(expected = IllegalArgumentException)
	void release_nonZeroMicroForAlpha() {
		Version.parseReleaseVersion('7.15.5.Alpha1')
	}

	@Test(expected = IllegalArgumentException)
	void release_nonZeroMicroForBeta() {
		Version.parseReleaseVersion('7.15.5.Beta2')
	}

	@Test(expected = IllegalArgumentException)
	void release_nonZeroMicroForCR() {
		Version.parseReleaseVersion('7.15.5.CR3')
	}

	@Test(expected = IllegalArgumentException)
	void development_nosnapshot() {
		Version.parseDevelopmentVersion('7.15.5')
	}

	@Test(expected = IllegalArgumentException)
	void development_extraQualifier() {
		Version.parseDevelopmentVersion('7.15.5.Final-SNAPSHOT')
	}

	private void testReleaseVersion(String versionString, String major, String minor, String micro, String qualifier) {
		Version version = Version.parseReleaseVersion(versionString)
		assertVersion(version, versionString, major, minor, micro, qualifier, false)
	}

	private void testDevelopmentVersion(String versionString, String major, String minor, String micro, String qualifier) {
		Version version = Version.parseDevelopmentVersion(versionString)
		assertVersion(version, versionString, major, minor, micro, qualifier, true)
	}

	private void assertVersion(Version version, String toString, String major, String minor, String micro, String qualifier, boolean snapshot) {
		Assert.assertEquals(toString, version.toString())
		Assert.assertEquals(major, version.major)
		Assert.assertEquals(minor, version.minor)
		Assert.assertEquals(micro, version.micro)
		Assert.assertEquals(qualifier, version.qualifier)
		Assert.assertEquals(snapshot, version.snapshot)
	}

}
