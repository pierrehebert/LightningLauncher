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


echo "beta? y/n [n]"
read answer
if [ "$answer" = "y" ]; then
	dir=api-beta
else
	dir=api
fi
rm -rf llx-$dir
cp -r html llx-$dir
tar czf llx-$dir.tar.gz llx-$dir
scp llx-$dir.tar.gz pierrot@vmail:/home/www/lightninglauncher.com/docs/www/scripting/reference/downloadable
ssh pierrot@vmail "cd /home/www/lightninglauncher.com/docs/www/scripting/reference && rm -rf $dir && tar xf downloadable/llx-$dir.tar.gz && mv llx-$dir $dir"
rm -rf llx-$dir.tar.gz llx-$dir
