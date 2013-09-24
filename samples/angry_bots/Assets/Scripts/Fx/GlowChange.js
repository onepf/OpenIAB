
#pragma strict

public var signalsNeeded : int = 1;

function OnSignal () {
	signalsNeeded--;
	if (signalsNeeded == 0) {
		renderer.material.SetColor ("_TintColor", Color (0.29, 0.64, 0.15, 0.5));
		enabled = false;
	}
}