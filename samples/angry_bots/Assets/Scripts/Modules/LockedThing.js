#pragma strict

// This component will forward a signal only if all the locks are unlocked

var locks : Lock[];
var conditionalSignal : SignalSender;

function OnSignal () {
	var locked : boolean = false;
	for (var lockObj : Lock in locks) {
		if (lockObj.locked)
			locked = true;
	}
	
	if (locked == false)
		conditionalSignal.SendSignals (this);
}
