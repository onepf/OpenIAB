
#pragma strict

public var triggerTag : String = "Player";
public var enterSignals : SignalSender;
public var exitSignals : SignalSender;

function OnTriggerEnter (other : Collider) {
	if (other.isTrigger)
		return;
	
	if (other.gameObject.tag == triggerTag || triggerTag == "") {
		enterSignals.SendSignals (this);
	}
}

function OnTriggerExit (other : Collider) {
	if (other.isTrigger)
		return;
	
	if (other.gameObject.tag == triggerTag || triggerTag == "") {
		exitSignals.SendSignals (this);
	}
}
