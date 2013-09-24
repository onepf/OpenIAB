
#pragma strict

var clip : String = "MyAnimation";
var speed : float = 1.0;

function OnSignal () {
	FixTime ();
	
	PlayWithSpeed ();
}

function OnPlay () {
	FixTime ();
	
	// Set the speed to be positive
	speed = Mathf.Abs (speed);
	
	PlayWithSpeed ();
}

function OnPlayReverse () {
	FixTime ();
	
	// Set the speed to be negative
	speed = Mathf.Abs (speed) * -1;
	
	PlayWithSpeed ();
}

private function PlayWithSpeed () {	
	// Play the animation with the desired speed.
	animation[clip].speed = speed;
	animation[clip].weight = 1;
	animation[clip].enabled = true;
	
	// Reverse the speed so it's ready for playing the other way next time.
	speed = -speed;
}

private function FixTime () {
	// If the animation played to the end last time, it got automatically rewinded.
	// We don't want that here, so set the time back to 1.
	if (speed < 0 && animation[clip].time == 0)
		animation[clip].normalizedTime = 1;
	
	// In other cases, just clamp the time so it doesn't exceed the bounds of the animation.
	else
		animation[clip].normalizedTime = Mathf.Clamp01 (animation[clip].normalizedTime);
}
