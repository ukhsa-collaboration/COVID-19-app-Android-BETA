# Sonar Colocate for Android

## Running tests

1. We need a fresh install of the app when running tests
   so that we can correctly simulate acceptance of permissions.

    When running from the command line it's already taken care of
    with the following gradle configuration -
 
    ```groovy
    afterEvaluate {
        tasks["connectedAndroidTest"].dependsOn("uninstallAll")
        tasks["packageDebugAndroidTest"].mustRunAfter("uninstallAll")
    }
    ```
    
    In Android Studio make sure to configure `androidTest` tasks to run `uninstallAll`
    from gradle before running.
    
    [androidTest setup]: docs/uninstall-all-before-android-tests.png

1. Device setup

    Make sure all animations are turned off on the device that is running the tests.
    https://developer.android.com/training/testing/espresso/setup#set-up-environment
