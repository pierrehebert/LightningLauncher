adb shell "su -c 'ls /data/app'" | grep net.pierrox.lightning_launcher.lp | cut -d"." -f1-5 | cut -f1 -d"-" | (while read l; do echo $l; adb uninstall $l; done)
