#!/bin/sh
sed -i -b s/"db-INSERT-APP-KEY-HERE"/"db-$1"/ app/src/main/AndroidManifest.xml
sed -i -b s/"INSERT_APP_KEY_HERE"/"$1"/ app/src/main/java/ru/orangesoftware/financisto/export/dropbox/Dropbox.java
echo "Done, don't forget to call git reset --hard after building apk"