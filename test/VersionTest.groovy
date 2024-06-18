/*
 * Hibernate Helpers for Jenkins pipelines
 *
 * License: Apache License, version 2 or later.
 * See the LICENSE.txt file in the root directory or <https://www.apache.org/licenses/LICENSE-2.0>.
 */


import org.hibernate.jenkins.pipeline.helpers.version.Version

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatThrownBy
import static org.junit.jupiter.api.Assumptions.assumeTrue

class VersionTest {

	static Stream<?> params() {
		return Arrays.stream(Version.Scheme.values() + [null])
	}

	@Test
	void releaseCR() {
		testReleaseVersion('6.6.0.CR1', null, '6', '6', '0', 'CR1')
		assertThat( doParseReleaseVersion('6.6.0.CR1', null).toString() ).startsWith( doParseDevelopmentVersion('6.6.0-SNAPSHOT', null).family + '.' )
		assertThat( doParseReleaseVersion('6.6.0.CR1', null).withoutFinalQualifier ).isEqualTo( "6.6.0.CR1" )
		assertThat( doParseReleaseVersion('6.6.0.CR1', null).tagName ).isEqualTo( "6.6.0.CR1" )
	}

	@Test
	void releaseFinal() {
		testReleaseVersion('6.6.0.Final', null, '6', '6', '0', 'Final')
		assertThat( doParseReleaseVersion('6.6.0.Final', null).toString() ).startsWith( doParseDevelopmentVersion('6.6.1-SNAPSHOT', null).family + '.' )
		assertThat( doParseReleaseVersion('6.6.0.Final', null).withoutFinalQualifier ).isEqualTo( "6.6.0" )
		assertThat( doParseReleaseVersion('6.6.0.Final', null).tagName ).isEqualTo( "6.6.0" )
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_valid(Version.Scheme scheme) {
		if (!scheme || scheme == Version.Scheme.JBOSS_CLASSIC) {
			testReleaseVersion('5.10.5.Final', scheme, '5', '10', '5', 'Final')
			testReleaseVersion('6.0.0.Final', scheme, '6', '0', '0', 'Final')
		}
		else {
			assert scheme == Version.Scheme.JBOSS_NO_FINAL
			testReleaseVersion('5.10.5', scheme, '5', '10', '5', null)
			testReleaseVersion('6.0.0', scheme, '6', '0', '0', null)
		}
		testReleaseVersion('6.0.0.Alpha1', scheme, '6', '0', '0', 'Alpha1')
		testReleaseVersion('6.0.0.Alpha12', scheme, '6', '0', '0', 'Alpha12')
		testReleaseVersion('6.0.0.Beta1', scheme, '6', '0', '0', 'Beta1')
		testReleaseVersion('6.0.0.Beta12', scheme, '6', '0', '0', 'Beta12')
		testReleaseVersion('6.0.0.CR1', scheme, '6', '0', '0', 'CR1')
		testReleaseVersion('6.0.0.CR12', scheme, '6', '0', '0', 'CR12')
	}

	@ParameterizedTest
	@MethodSource("params")
	void development_valid(Version.Scheme scheme) {
		testDevelopmentVersion('5.10.5-SNAPSHOT', scheme, '5', '10', '5', null)
		testDevelopmentVersion('6.0.0-SNAPSHOT', scheme, '6', '0', '0', null)
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_snapshot(Version.Scheme scheme) {
		assertThatThrownBy {
			doParseReleaseVersion('7.15.5-SNAPSHOT', scheme)
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_missingFinalQualifier(Version.Scheme scheme) {
		assumeTrue(scheme == null || scheme == Version.Scheme.JBOSS_CLASSIC,
				"This test is only relevant for the JBOSS_CLASSIC version scheme")
		assertThatThrownBy {
			doParseReleaseVersion('7.15.7', scheme)
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_extraFinalQualifier(Version.Scheme scheme) {
		assumeTrue(scheme == Version.Scheme.JBOSS_NO_FINAL,
				"This test is only relevant for the NO_FINAL version scheme")
		assertThatThrownBy {
			doParseReleaseVersion('7.15.7.Final', scheme)
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_missingMicro(Version.Scheme scheme) {
		assertThatThrownBy {
			if (!scheme || scheme == Version.Scheme.JBOSS_CLASSIC) {
				doParseReleaseVersion('7.15.Final', scheme)
			}
			else {
				assert scheme == Version.Scheme.JBOSS_NO_FINAL
				doParseReleaseVersion('7.15', scheme)
			}
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_missingMinorAndMicro(Version.Scheme scheme) {
		assertThatThrownBy {
			if (!scheme || scheme == Version.Scheme.JBOSS_CLASSIC) {
				doParseReleaseVersion('7.Final', scheme)
			}
			else {
				assert scheme == Version.Scheme.JBOSS_NO_FINAL
				doParseReleaseVersion('7', scheme)
			}
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_unknownQualifier(Version.Scheme scheme) {
		assertThatThrownBy {
			doParseReleaseVersion('7.15.5.Foo', scheme)
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_finalQualifierWithNumber(Version.Scheme scheme) {
		assertThatThrownBy {
			doParseReleaseVersion('7.15.5.Final2', scheme)
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_nonZeroMicroForAlpha(Version.Scheme scheme) {
		assertThatThrownBy {
			doParseReleaseVersion('7.15.5.Alpha1', scheme)
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_nonZeroMicroForBeta(Version.Scheme scheme) {
		assertThatThrownBy {
			doParseReleaseVersion('7.15.5.Beta2', scheme)
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void release_nonZeroMicroForCR(Version.Scheme scheme) {
		assertThatThrownBy {
			doParseReleaseVersion('7.15.5.CR3', scheme)
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void development_nosnapshot(Version.Scheme scheme) {
		assertThatThrownBy {
			doParseDevelopmentVersion('7.15.5', scheme)
		}
				.isInstanceOf(IllegalArgumentException)
	}

	@ParameterizedTest
	@MethodSource("params")
	void development_extraQualifier(Version.Scheme scheme) {
		assertThatThrownBy {
			doParseDevelopmentVersion('7.15.5.Final-SNAPSHOT', scheme)
		}
				.isInstanceOf(IllegalArgumentException)
	}

	private Version doParseReleaseVersion(String versionString, Version.Scheme scheme) {
		scheme ? Version.parseReleaseVersion(versionString, scheme) : Version.parseReleaseVersion(versionString)
	}

	private Version doParseDevelopmentVersion(String versionString, Version.Scheme scheme) {
		scheme ? Version.parseDevelopmentVersion(versionString, scheme) : Version.parseDevelopmentVersion(versionString)
	}

	private void testReleaseVersion(String versionString, Version.Scheme scheme, String major, String minor, String micro, String qualifier) {
		Version version = doParseReleaseVersion(versionString, scheme)
		assertVersion(version, versionString, major, minor, micro, qualifier, false)
	}

	private void testDevelopmentVersion(String versionString, Version.Scheme scheme, String major, String minor, String micro, String qualifier) {
		Version version = doParseDevelopmentVersion(versionString, scheme)
		assertVersion(version, versionString, major, minor, micro, qualifier, true)
	}

	private static void assertVersion(Version version, String toString, String major, String minor, String micro, String qualifier, boolean snapshot) {
		assertThat(toString).isEqualTo(version.toString())
		assertThat(toString).startsWith(version.family)
		assertThat(major).isEqualTo(version.major)
		assertThat(minor).isEqualTo(version.minor)
		assertThat(micro).isEqualTo(version.micro)
		assertThat(qualifier).isEqualTo(version.qualifier)
		assertThat(snapshot).isEqualTo(version.snapshot)
	}

}
