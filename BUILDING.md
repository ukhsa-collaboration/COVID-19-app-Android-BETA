Installs:
  (make sure you've got 3-4Gb of diskspace free - especially if you install any emulators)

  * [Install the JDK](https://jdk.java.net/14/) into a dir
  * set the environment variable JAVA_HOME to the dir you unzipped the JDK to.
  * Download and install [Android Studio](https://developer.android.com/studio)
  
Setup:

  (Don't attempt to build before you've done the following steps or you'll have to clean before rebuilding
  as it may still pick up the nhs app id)

  * Go to [firebase](https://console.firebase.google.com/) and set up an account.
  * In firebase create a new android app with a unique app id - E.g `test.android.covid86`
  * Save the google-services.json that it gives you to ./app/google-services.json
  * In `app\build.gradle` replace `uk.nhs.nhsx.colocate` with your firebase app id (E.g. `test.android.covid86`).
  * In `app\build.gradle` comment out the throw exceptions and just any string:
```
        def baseUrl = findProperty("sonar.baseUrl") ?: {
            ""
            //throw new MissingPropertyException("Missing property `sonar.baseUrl` please see README for instructions")
        }

        def headerValue = findProperty("sonar.headerValue") ?: {
            ""
            //throw new MissingPropertyException("Missing property `sonar.headerValue` please see README for instructions")
        }

        def analyticsKey = findProperty("sonar.analyticsKey") ?: {
            ""
            //throw new MissingPropertyException("Missing property `sonar.analyticsKey` please see README for instructions")
        }
```

  * If you don't plug an android phone into a usb, [create an emulator](https://developer.android.com/studio/run/managing-avds#createavd)

  * At this point running `gradlew` should build cleanly and you should be able to run the app by pressing play in android studio
