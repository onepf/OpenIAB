#pragma strict

var locked : boolean = true;

var unlockedSignal : SignalSender;

function OnSignal () {
	if (locked) {
		locked = false;
		unlockedSignal.SendSignals (this);
	}
}
