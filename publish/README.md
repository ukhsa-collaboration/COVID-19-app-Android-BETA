# Publish Builds in Play Store

### Requirements

##### Github personal access token

You need to have a personal access token, using your own github account, and provide it together with
your username via `GITHUB_USER_TOKEN` environment variable.

```sh
$ export GITHUB_USER_TOKEN = <user>:token
```

## Publish

In order to publish a new build, you need to run `trigger.sh` script.

Script needs two arguments:
 - First one is the publish track. valid values are `test` or `alpha`
 - Second one is a git ref. it can be a commit sha, tag or any other valid git ref.

### Publish Test Build (Using test endpoints)

```sh
$ ./trigger.sh test 376e3f4
```

### Publish Alpha Build (Using production endpoints)

```sh
$ ./trigger.sh alpha 376e3f4
```
