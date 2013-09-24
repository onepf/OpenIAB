
public var autoDisableAfter : float = 2.0;

function OnEnable() {					
	DeactivateCoroutine (autoDisableAfter);	
}

function DeactivateCoroutine(t : float) {
	yield WaitForSeconds(t);
	
	gameObject.SetActiveRecursively(false);
}

