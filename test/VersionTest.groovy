/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */

import org.hibernate.jenkins.pipeline.helpers.version.Version
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized.class)
class VersionTest {

	@Parameterized.Parameters(name = "{0}")
	static Version.Scheme[] params() {
		return Version.Scheme.values() + [null]
	}

	@Parameterized.Parameter
	public Version.Scheme scheme

	@Test
	void release_valid() {
		if (!scheme || scheme == Version.Scheme.JBOSS_CLASSIC) {
			testReleaseVersion('5.10.5.Final', '5', '10', '5', 'Final')
			testReleaseVersion('6.0.0.Final', '6', '0', '0', 'Final')
		}
		else {
			assert scheme == Version.Scheme.JBOSS_NO_FINAL
			testReleaseVersion('5.10.5', '5', '10', '5', null)
			testReleaseVersion('6.0.0', '6', '0', '0', null)
		}
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
		doParseReleaseVersion('7.15.5-SNAPSHOT')
	}

	@Test(expected = IllegalArgumentException)
	void release_missingFinalQualifier() {
		Assume.assumeTrue("This test is only relevant for the JBOSS_CLASSIC version scheme",
				scheme == null || scheme == Version.Scheme.JBOSS_CLASSIC)
		doParseReleaseVersion('7.15.7')
	}

	@Test(expected = IllegalArgumentException)
	void release_extraFinalQualifier() {
		Assume.assumeTrue("This test is only relevant for the NO_FINAL version scheme",
				scheme == Version.Scheme.JBOSS_NO_FINAL)
		doParseReleaseVersion('7.15.7.Final')
	}

	@Test(expected = IllegalArgumentException)
	void release_missingMicro() {
		if (!scheme || scheme == Version.Scheme.JBOSS_CLASSIC) {
			doParseReleaseVersion('7.15.Final')
		}
		else {
			assert scheme == Version.Scheme.JBOSS_NO_FINAL
			doParseReleaseVersion('7.15')
		}
	}

	@Test(expected = IllegalArgumentException)
	void release_missingMinorAndMicro() {
		if (!scheme || scheme == Version.Scheme.JBOSS_CLASSIC) {
			doParseReleaseVersion('7.Final')
		}
		else {
			assert scheme == Version.Scheme.JBOSS_NO_FINAL
			doParseReleaseVersion('7')
		}
	}

	@Test(expected = IllegalArgumentException)
	void release_unknownQualifier() {
		doParseReleaseVersion('7.15.5.Foo')
	}

	@Test(expected = IllegalArgumentException)
	void release_finalQualifierWithNumber() {
		doParseReleaseVersion('7.15.5.Final2')
	}

	@Test(expected = IllegalArgumentException)
	void release_nonZeroMicroForAlpha() {
		doParseReleaseVersion('7.15.5.Alpha1')
	}

	@Test(expected = IllegalArgumentException)
	void release_nonZeroMicroForBeta() {
		doParseReleaseVersion('7.15.5.Beta2')
	}

	@Test(expected = IllegalArgumentException)
	void release_nonZeroMicroForCR() {
		doParseReleaseVersion('7.15.5.CR3')
	}

	@Test(expected = IllegalArgumentException)
	void development_nosnapshot() {
		doParseDevelopmentVersion('7.15.5')
	}

	@Test(expected = IllegalArgumentException)
	void development_extraQualifier() {
		doParseDevelopmentVersion('7.15.5.Final-SNAPSHOT')
	}

	private Version doParseReleaseVersion(String versionString) {
		scheme ? Version.parseReleaseVersion(versionString, scheme) : Version.parseReleaseVersion(versionString)
	}

	private Version doParseDevelopmentVersion(String versionString) {
		scheme ? Version.parseDevelopmentVersion(versionString, scheme) : Version.parseDevelopmentVersion(versionString)
	}

	private void testReleaseVersion(String versionString, String major, String minor, String micro, String qualifier) {
		Version version = doParseReleaseVersion(versionString)
		assertVersion(version, versionString, major, minor, micro, qualifier, false)
	}

	private void testDevelopmentVersion(String versionString, String major, String minor, String micro, String qualifier) {
		Version version = doParseDevelopmentVersion(versionString)
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
