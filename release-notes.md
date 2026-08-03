# Release Notes

## v0.2.1

# Release Notes for Version 0.2.1

## New Features
- **MyBatis Resilience4j Plugin**: Added MyBatis integration with Resilience4j and corresponding tests. ([a04b81e](https://github.com/your-repo/commit/a04b81e))
- **AspectJ Weaver Test Dependency**: Introduced AspectJ weaver as a test dependency. ([a3803c8](https://github.com/your-repo/commit/a3803c8))
- **Microsphere Java Test**: Added microsphere-java-test as a test dependency. ([6e7752c](https://github.com/your-repo/commit/6e7752c))
- **Contributor Code of Conduct**: Added a Contributor Code of Conduct document to improve collaboration. ([01be549](https://github.com/your-repo/commit/01be549))
- **Upstream Branch Sync Workflow**: Added CI workflow to sync upstream branches. ([c915251](https://github.com/your-repo/commit/c915251))

## Bug Fixes
- **Indentation Fix for Dependabot**: Corrected indentation in `.github/dependabot.yml`. ([b9318b4](https://github.com/your-repo/commit/b9318b4))

## Other Changes
- **Module and Parent Updates**: Bumped microsphere module versions and parent POM. ([1a21f8b](https://github.com/your-repo/commit/1a21f8b), [d341302](https://github.com/your-repo/commit/d341302))
- **Resilience4j Dependency Updates**: Renamed resilience4j dependency artifact IDs for clarity. ([0fcee6c](https://github.com/your-repo/commit/0fcee6c), [3369352](https://github.com/your-repo/commit/3369352))
- **README Enhancements**: Revamped README with a project overview, updated Resilience4j links, and added documentation improvements. ([536860c](https://github.com/your-repo/commit/536860c), [2ea3afd](https://github.com/your-repo/commit/2ea3afd))
- **Maven Updates**: Updated Maven wrapper to version 3.9.15 and improved CI workflows. ([889c606](https://github.com/your-repo/commit/889c606), [b60f6d8](https://github.com/your-repo/commit/b60f6d8))
- **Codecov Badge Update**: Adjusted Codecov badge branch reference to `main`. ([d4505ed](https://github.com/your-repo/commit/d4505ed))
- **Revision Input Description**: Clarified descriptions for revision input fields. ([abc6a53](https://github.com/your-repo/commit/abc6a53))

---

Thank you for using version 0.2.1! 🚀

## v0.2.2

# Release Notes - Version 0.2.2

## New Features
- **Hibernate Support:** Added Hibernate core plugin and test utilities. ([d827708](https://github.com/microsphere-projects/microsphere-resilience4j/commit/d827708))
- **Microsphere OpenFeign Support:** Introduced optional Microsphere OpenFeign dependencies. ([e696ef0](https://github.com/microsphere-projects/microsphere-resilience4j/commit/e696ef0))
- **Annotation Processor:** Added optional annotation processor dependency for usability. ([64e3ec7](https://github.com/microsphere-projects/microsphere-resilience4j/commit/64e3ec7))

## Bug Fixes
- **Class Renaming:** Renamed `EnableResilience4jExtension` to `EnableResilience4j` for consistency. ([d9aec8b](https://github.com/microsphere-projects/microsphere-resilience4j/commit/d9aec8b))
- **Method Renaming:** Renamed `setContext()` to `withinContext()` for clarity. ([fabd0e2](https://github.com/microsphere-projects/microsphere-resilience4j/commit/fabd0e2))
- Fixed template loading to respect selected modules. ([89e6e9a](https://github.com/microsphere-projects/microsphere-resilience4j/commit/89e6e9a))

## Documentation
- Updated README to clarify web extension references and revised branch names. ([5926a09](https://github.com/microsphere-projects/microsphere-resilience4j/commit/5926a09), [9a0d579](https://github.com/microsphere-projects/microsphere-resilience4j/commit/9a0d579))
- Fixed DeepWiki badge link. ([4a83d26](https://github.com/microsphere-projects/microsphere-resilience4j/commit/4a83d26))

## Dependency Updates
- Bumped **microsphere-spring-cloud-parent** to `0.2.12`. ([ad26634](https://github.com/microsphere-projects/microsphere-resilience4j/commit/ad26634))
- Updated **microsphere-alibaba-druid**, **microsphere-mybatis**, and **microsphere-redis** dependencies. ([897d139](https://github.com/microsphere-projects/microsphere-resilience4j/commit/897d139), [b9a3817](https://github.com/microsphere-projects/microsphere-resilience4j/commit/b9a3817), [68ad659](https://github.com/microsphere-projects/microsphere-resilience4j/commit/68ad659))

## Test Improvements
- Used static imports across various test classes for better readability. ([6d59dc4](https://github.com/microsphere-projects/microsphere-resilience4j/commit/6d59dc4), [6301c5a](https://github.com/microsphere-projects/microsphere-resilience4j/commit/6301c5a), [a48fbf5](https://github.com/microsphere-projects/microsphere-resilience4j/commit/a48fbf5))
- Removed unused imports and refined test configurations. ([0a23303](https://github.com/microsphere-projects/microsphere-resilience4j/commit/0a23303), [2721c7a](https://github.com/microsphere-projects/microsphere-resilience4j/commit/2721c7a))
- Deleted unused `logback.xml` from the test directory. ([314055c](https://github.com/microsphere-projects/microsphere-resilience4j/commit/314055c))

## Build and Workflow Enhancements
- Configured OSSRH credentials in the publish workflow. ([6839aae](https://github.com/microsphere-projects/microsphere-resilience4j/commit/6839aae))
- Optimized POM files: Aligned plugin dependency scopes, removed unused configurations, and cleaned up extra blank lines. ([587a512](https://github.com/microsphere-projects/microsphere-resilience4j/commit/587a512), [7b99c53](https://github.com/microsphere-projects/microsphere-resilience4j/commit/7b99c53), [409eb74](https://github.com/microsphere-projects/microsphere-resilience4j/commit/409eb74))
- Updated Maven workflows and streamlined scripts. ([5eb72ff](https://github.com/microsphere-projects/microsphere-resilience4j/commit/5eb72ff))
- Improved Dependabot configuration formatting. ([2cff3c2](https://github.com/microsphere-projects/microsphere-resilience4j/commit/2cff3c2))

## Other Changes
- General code cleanup: Removed duplicate line separators, standardized whitespace, and formatted annotations. ([64578b0](https://github.com/microsphere-projects/microsphere-resilience4j/commit/64578b0), [2721c7a](https://github.com/microsphere-projects/microsphere-resilience4j/commit/2721c7a)) 

--- 

This release includes several improvements and dependency updates, alongside better testing practices and documentation updates for a cleaner and more robust codebase. 

**Full Changelog**: https://github.com/microsphere-projects/microsphere-resilience4j/compare/0.2.1...0.2.2