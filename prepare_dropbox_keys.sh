#!/bin/sh
sed -i -b s/"db-INSERT-APP-KEY-HERE"/"db-$1"/ AndroidManifest.xml
sed -i -b s/"INSERT_APP_KEY_HERE"/"$1"/ src/ru/orangesoftware/financisto/export/dropbox/Dropbox.java
sed -i -b s/"INSERT_APP_SECRET_HERE"/"$2"/ src/ru/orangesoftware/financisto/export/dropbox/Dropbox.java
echo "Done, don't forget to call bzr revert after building apk"