#pragma strict
#pragma downcast
// (#pragma downcast is required to iterate through children of a transform)

import System.Collections.Generic;

// The object prefab will be spawned on the locations of each of the child transforms of this object

var objectToSpawn : GameObject;
var delayInBetween : float = 0;
var onDestroyedSignals : SignalSender;

private var spawned : List.<GameObject> = new List.<GameObject> ();

// Keep disabled from the beginning
enabled = false;

// When we get a signal, spawn the objectToSpawn objects and store them.
// Also enable this behaviour so the Update function will be run.
function OnSignal () {
	for (var child : Transform in transform) {
		// Spawn with the position and rotation of the child transform
		spawned.Add (Spawner.Spawn (objectToSpawn, child.position, child.rotation));
		
		// Delay
		yield WaitForSeconds (delayInBetween);
	}
	enabled = true;
}

// After the objects are spawned, check each frame if they're still there.
// Once they're not, 
function Update () {
	// Once the list is empty, activate the onDestroyedSignals and disable again.
	if (spawned.Count == 0) {
		onDestroyedSignals.SendSignals (this);
		enabled = false;
	}
	// As long as the list is not empty, check if the first object in the list
	// has been destroyed, and remove it from the list if it has.
	// We don't need to check the rest of the list. All of the entries will
	// end up being the first one eventually.
	// Note that only one object can be removed per frame, so if there's
	// a really high amount, there may be a slight delay before the list is empty.
	else if (spawned[0] == null || spawned[0].active == false) {
		spawned.RemoveAt (0);
	}
}
