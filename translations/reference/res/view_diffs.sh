lang=$1

cd values-$lang
last=`ls -tr strings.1* | tail -1`
echo $last
diff strings.xml $last | more
