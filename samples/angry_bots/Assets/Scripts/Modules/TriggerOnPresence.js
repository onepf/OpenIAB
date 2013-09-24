#pragma strict

public var enterSignals : SignalSender;
public var exitSignals : SignalSender;

public var objects : System.Collections.Generic.List.<GameObject>;

function Awake () {
	objects = new System.Collections.Generic.List.<GameObject> ();
	enabled = false;
}

function OnTriggerEnter (other : Collider) {
	if (other.isTrigger)
		return;
	
	var wasEmpty : boolean = (objects.Count == 0);
	
	objects.Add (other.gameObject);
	
	if (wasEmpty) {
		enterSignals.SendSignals (this);
		enabled = true;
	}
}

function OnTriggerExit (other : Collider) {
	if (other.isTrigger)
		return;
	
	if (objects.Contains (other.gameObject))
		objects.Remove (other.gameObject);
	
	if (objects.Count == 0) {
		exitSignals.SendSignals (this);
		enabled = false;
	}
}
