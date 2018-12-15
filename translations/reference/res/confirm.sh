lang=$1

cd values-$lang
last=`ls -tr strings.1* | tail -1`
echo $last
cp $last strings.xml
rm -f strings.1*
svn ci strings.xml -m "Translations $lang"
