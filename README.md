# Sonar Co-Locate for Android

## Running tests

1. Device setup

    Make sure all animations are turned off on the device (not an emulator) that is running the tests.
    https://developer.android.com/training/testing/espresso/setup#set-up-environment

1. Before pushing code run -
    ```bash
    ./gradlew build connectedAndroidTest
    ```

   They are the default tasks, so you can also just run -
   ```bash
   ./gradlew
   ```

**NOTE**
 * DO NOT UNDER ANY CIRCUMSTANCE skip running the tests before pushing.
 * DO NOT UNDER ANY CIRCUMSTANCE disable tests.
 * DO NOT UNDER ANY CIRCUMSTANCE push code to master that knowingly breaks the test suite.

## Code formatting with KTLint

KTLint has been added to the build and will automatically run when you run the build.
It is attached to the `check` step of the build.

The official Kotlin style guide recommends slightly different configuration from the default
Android Studio setup.

1. The continuation indent should be set to 4

    ![Continuation indent](docs/kotlin-continuation-indent.png)

1. Imports should *never* use wildcards

    ![No wildcard import](docs/kotlin-import-no-wildcards.png)

1. Files should end with a new line character.
    
    ![Ensure line feed on save](docs/kotlin-newline-character.png)
