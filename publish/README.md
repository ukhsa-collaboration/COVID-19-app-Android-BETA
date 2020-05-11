# Publish Builds in Play Store

### Requirements

##### Github personal access token

You need to have a personal access token, using your own github account, and provide it together with
your username via `GITHUB_USER_TOKEN` environment variable.

```sh
$ export GITHUB_USER_TOKEN = <user>:token
```

## Publish Internal

In order to publish a new internal build, you need to run `internal.sh` script.

Script needs an arguments which is a git ref (commit-sha, branch ...).

```sh
$ ./internal 376e3f
```

Above script publishes a build to `internal` track in play store.

### Publish Staging and Alpha

In order to publish a new staging and alpha build, you need to run `release.sh` script.

Script needs two arguments:
 - The first one is a git ref (commit-sha, branch ...)
 - The second one is the release tag for github release (e.g v1.1)

```sh
$ ./release.sh 376e3f
```

Above script publishes two builds:
  - the first one uses the staging endpoints and is published to `Staging` track in play store.
  - the second one uses the production endpoints and is published to `Alpha` track in play store.
