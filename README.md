# app-access-points
Simple Android app that shows on screen a list of access points seen by the phone

There are three important files in this app, and all of them can be found inside the folder /Test:
*/Test/app/src/main/java/com/android/test/MainActivity.java
This is the main activity, which only contains a button that will redirect the app to CheckWify.java

*/Test/app/src/main/java/com/android/test/CheckWifi.java
This is the most important file. In this class there are two methods:
  *enableWifi() - Checks whether WiFi is enabled or not, and in case it's not enabled it will turn it on.
  *wifiProbe(int tolerance) - Returns a String containing a list of access points seen by the phone, in order of signal power. It's possible to introduce a tolerance which is the maximum difference between the access point with Max power and the access point with Min power.
  
*/Test/app/src/main/java/com/android/test/WifiScanReceiver.java
This is just a helper class necessary for the method wifiProbe inside the class CheckWifi.
