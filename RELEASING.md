- [Overview](#overview)
- [Branching](#branching)
  - [Release Branching](#release-branching)
  - [Feature Branches](#feature-branches)
- [Release Labels](#release-labels)
- [Releasing](#releasing)
- [Snapshots](#snapshot-builds)

## Overview

This document explains the release strategy for artifacts in this organization.

## Branching

### Release Branching

Given the current major release of 1.0, projects in this organization maintain the following active branches.

* **main**: The next _major_ release. This is the branch where all merges take place and code moves fast.
* **1.x**: The next _minor_ release. Once a change is merged into `main`, decide whether to backport it to `1.x`.
* **1.0**: The _current_ release. In between minor releases, only hotfixes (e.g. security) are backported to `1.0`.

Label PRs with the next major version label (e.g. `2.0.0`) and merge changes into `main`. Label PRs that you believe need to be backported as `1.x` and `1.0`. Backport PRs by checking out the versioned branch, cherry-pick changes and open a PR against each target backport branch.

### Feature Branches

Do not create branches in the upstream repo, use your fork, for the exception of long lasting feature branches that require active collaboration from multiple developers. Name feature branches `feature/<thing>`. Once the work is merged to `main`, please make sure to delete the feature branch.

## Release Labels

Repositories create consistent release labels, such as `v1.0.0`, `v1.1.0` and `v2.0.0`, as well as `patch` and `backport`. Use release labels to target an issue or a PR for a given release.

## Releasing

The release process is standard across repositories in this org and is run by a release manager volunteering from amongst [maintainers](MAINTAINERS.md).

1. Create a tag, e.g. 1.0.0, and push it to this GitHub repository.
1. The [release-drafter.yml](.github/workflows/release-drafter.yml) will be automatically kicked off and a draft release will be created.
1. This draft release triggers the [jenkins release workflow](https://build.ci.opensearch.org/job/spring-data-opensearch-release) as a result of which the client is released on [maven central](https://search.maven.org/search?q=org.opensearch.client). Please note that the release workflow is triggered only if created release is in draft state.
1. Once the above release workflow is successful, the drafted release on GitHub is published automatically.
1. Increment "version" in [version.properties](./version.properties) to the next iteration, e.g. v2.1.1. See [example](https://github.com/opensearch-project/spring-data-opensearch/pull/75).

## Snapshot Builds
The [snapshots builds](https://aws.oss.sonatype.org/content/repositories/snapshots/org/opensearch/client/opensearch-java/) are published to sonatype using [publish-snapshots.yml](./.github/workflows/publish-snapshots.yml) workflow. Each `push` event to the main branch triggers this workflow.