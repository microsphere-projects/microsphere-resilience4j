# Release Notes

## v0.1.1

# Release Notes for v0.1.1

## New Features
- Renamed `feign` to `openfeign` with updated documentation and dependencies. ([b7e4195](#))
- Improved release flow with release notes generation. ([da51f37](#))

## Bug Fixes
- Fixed `dependabot.yml` indentation for updates. ([ddac213](#))

## Other Changes
- Updated Maven wrapper to 3.9.15. ([eb2d4db](#))
- Bumped parent POM version to 0.1.10. ([aaf94b2](#))
- Updated Microsphere module versions in parent POM. ([bd2cca0](#))
- Updated DeepWiki badge link. ([04ebc7f](#))
- Granted `contents: read` permission to workflows. ([dfdf940](#))

--- 

## v0.1.2

# Release Notes - Version 0.1.2

## New Features
- Added Hibernate plugin with aligned plugin dependencies. ([913231f](#))
- Introduced MyBatis Resilience4j plugin and test suite. ([cf3dc3b](#))
- Replaced custom annotations with Microsphere annotations and added `ThreadLocal` support. ([7b56cef](#))

## Bug Fixes
- Fixed incorrect `groupId` for Hibernate core dependency. ([7cfb88b](#))
- Renamed `setContext` to `withinContext` and updated related tests for clarity. ([6361c3e](#))
- Renamed `EnableResilience4jExtension` annotation for consistency. ([7f405be](#))

## Documentation
- Updated version numbers and branch names in `README.md`. ([d894166](#), [270dbc5](#))

## Dependency Updates
- Downgraded `microsphere-alibaba-druid` version. ([73bba1d](#))
- Bumped Microsphere integration and parent POM versions. ([f22923e](#), [3cb1af5](#), [ea1d423](#), [52f6806](#), [b8151e9](#))
- Imported Microsphere BOMs into the parent POM to streamline dependency management. ([a175b33](#))

## Test Improvements
- Added Java 25 to CI Maven build matrix. ([b1e8d1f](#))
- Removed unnecessary `test logback.xml` file. ([0dcae99](#))

## Build and Workflow Enhancements
- Fixed Maven workflows in CI pipelines. ([12dd49b](#))
- Reverted to `javax.servlet` API and enforced Java 11 compatibility. ([1cdfd12](#))
- Adjusted Maven dependencies and resolved EOF formatting issues. ([d6f2bac](#))

## Other Changes
- Merged updates from `release-1.x` into `dev-1.x`. ([f6bd122](#))
- Bumped version to the next patch after publishing v0.1.1. ([80d48fe](#))

---

**Full Changelog**: https://github.com/microsphere-projects/microsphere-resilience4j/compare/0.1.1...0.1.2