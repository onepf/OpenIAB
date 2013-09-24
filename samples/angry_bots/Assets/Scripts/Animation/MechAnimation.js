#pragma strict

var rigid : Rigidbody;
var idle : AnimationClip;
var walk : AnimationClip;
var turnLeft : AnimationClip;
var turnRight : AnimationClip;
var footstepSignals : SignalSender;

private var tr : Transform;
private var lastFootstepTime : float = 0;
private var lastAnimTime : float = 0;

function OnEnable () {
	tr = rigid.transform;
	
	animation[idle.name].layer = 0;
	animation[idle.name].weight = 1;
	animation[idle.name].enabled = true;
	
	animation[walk.name].layer = 1;
	animation[turnLeft.name].layer = 1;
	animation[turnRight.name].layer = 1;
	
	animation[walk.name].weight = 1;
	animation[turnLeft.name].weight = 0;
	animation[turnRight.name].weight = 0;
	
	animation[walk.name].enabled = true;
	animation[turnLeft.name].enabled = true;
	animation[turnRight.name].enabled = true;
	
	//animation.SyncLayer (1);
}

function FixedUpdate () {
	var turningWeight : float = Mathf.Abs (rigid.angularVelocity.y) * Mathf.Rad2Deg / 100.0;
	var forwardWeight : float = rigid.velocity.magnitude / 2.5;
	var turningDir : float = Mathf.Sign (rigid.angularVelocity.y);
	
	// Temp, until we get the animations fixed
	animation[walk.name].speed = Mathf.Lerp (1.0, animation[walk.name].length / animation[turnLeft.name].length * 1.33, turningWeight);
	animation[turnLeft.name].time = animation[walk.name].time;
	animation[turnRight.name].time = animation[walk.name].time;
	
	animation[turnLeft.name].weight = Mathf.Clamp01 (-turningWeight * turningDir);
	animation[turnRight.name].weight = Mathf.Clamp01 (turningWeight * turningDir);
	animation[walk.name].weight = Mathf.Clamp01 (forwardWeight);
	
	if (forwardWeight + turningWeight > 0.1) {
		var newAnimTime = Mathf.Repeat (animation[walk.name].normalizedTime * 2 + 0.1, 1);
		if (newAnimTime < lastAnimTime) {
			if (Time.time > lastFootstepTime + 0.1) {
				footstepSignals.SendSignals (this);
				lastFootstepTime = Time.time;
			}
		}
		lastAnimTime = newAnimTime;
	}
}
