LL.bindClass("android.os.BatteryManager");
LL.bindClass("android.content.BroadcastReceiver");
LL.bindClass("android.content.Intent");
LL.bindClass("android.content.IntentFilter");

// update every 10s, this is a cyclic update, could you a receiver instead
var PERIOD = 10000;

var item = LL.getEvent().getItem();

function refresh() {
	var ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	var batteryStatus = LL.getContext().registerReceiver(null, ifilter);

    var status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    var isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

    var chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
    var usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
    var acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

    var batstat = "";

    if (isCharging)
    {
        if (usbCharge)
        {
            batstat = "usb charging . . .";
        }else
        {
            batstat = "AC charging . . .";
        }
    }

    item.setLabel(batstat);

    item.setTag(setTimeout(refresh, PERIOD));
}

refresh();