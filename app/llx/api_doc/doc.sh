#export JAVA_HOME=/home/pierrot/Devel/Tools/jdk1.8.0_11
rm -rf html
javadoc \
  net.pierrox.lightning_launcher.script.api \
  ../app/src/main/java/net/pierrox/lightning_launcher/prefs/*.java \
  ../core/src/main/java/net/pierrox/lightning_launcher/script/api/*.java \
  ../core/src/main/java/net/pierrox/lightning_launcher/script/api/screen/*.java \
  ../core/src/main/java/net/pierrox/lightning_launcher/script/api/svg/*.java \
  ../core/src/main/java/net/pierrox/lightning_launcher/script/api/palette/*.java \
  -overview overview.html \
  -doclet com.google.doclava.Doclava -docletpath doclava-1.0.6.jar  \
  -bootclasspath $JAVA_HOME/jre/lib/rt.jar \
  -classpath /home/pierrot/Documents/Devel/Android/Sdk/platforms/android-23/android.jar \
  -public \
  -d html

sed -i 's/getEvent_/getEvent/g' html/reference/net/pierrox/lightning_launcher/script/api/Lightning.html
sed -i 's/getEvent_/getEvent/g' html/reference/net/pierrox/lightning_launcher/script/api/LL.html
sed -i 's/getEvent_/getEvent/g' html/reference/current.xml

scp -r html pierrot@ruby:~/tmp
