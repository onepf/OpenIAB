#pragma strict

var rigid : Rigidbody;
var forwardAnim : AnimationClip;
var backAnim : AnimationClip;
var leftAnim : AnimationClip;
var rightAnim : AnimationClip;

var walking : float;
var angle : float;

private var tr : Transform;

function OnEnable () {
	tr = rigid.transform;
	
	animation[forwardAnim.name].layer = 1;
	animation[forwardAnim.name].enabled = true;
	animation[backAnim.name].layer = 1;
	animation[backAnim.name].enabled = true;
	animation[leftAnim.name].layer = 1;
	animation[leftAnim.name].enabled = true;
	animation[rightAnim.name].layer = 1;
	animation[rightAnim.name].enabled = true;
	animation.SyncLayer (1);
}

function Update () {
	rigid.velocity = Quaternion.Euler(0, angle, 0) * rigid.transform.forward * 2.4 * walking;
	
	var velocity : Vector3 = rigid.velocity;
	velocity.y = 0;
	
	var walkWeight : float = velocity.magnitude / 2.4;
	
	animation[forwardAnim.name].speed = walkWeight;
	animation[rightAnim.name].speed = walkWeight;
	animation[backAnim.name].speed = walkWeight;
	animation[leftAnim.name].speed = walkWeight;
	
	if (velocity == Vector3.zero) {
		return;
	}
	
	var angle : float = Mathf.DeltaAngle (
		HorizontalAngle (tr.forward),
		HorizontalAngle (rigid.velocity)
	);
	
	var w : float;
	if (angle < -90) {
		w = Mathf.InverseLerp (-180, -90, angle);
		animation[forwardAnim.name].weight = 0;
		animation[rightAnim.name].weight = 0;
		animation[backAnim.name].weight = 1 - w;
		animation[leftAnim.name].weight = 1;
	}
	else if (angle < 0) {
		w = Mathf.InverseLerp (-90, 0, angle);
		animation[forwardAnim.name].weight = w;
		animation[rightAnim.name].weight = 0;
		animation[backAnim.name].weight = 0;
		animation[leftAnim.name].weight = 1 - w;
	}
	else if (angle < 90) {
		w = Mathf.InverseLerp (0, 90, angle);
		animation[forwardAnim.name].weight = 1 - w;
		animation[rightAnim.name].weight = w;
		animation[backAnim.name].weight = 0;
		animation[leftAnim.name].weight = 0;
	}
	else {
		w = Mathf.InverseLerp (90, 180, angle);
		animation[forwardAnim.name].weight = 0;
		animation[rightAnim.name].weight = 1 - w;
		animation[backAnim.name].weight = w;
		animation[leftAnim.name].weight = 0;
	}
}

static function HorizontalAngle (direction : Vector3) {
	return Mathf.Atan2 (direction.x, direction.z) * Mathf.Rad2Deg;
}

function OnGUI () {
	GUILayout.Label ("Angle (0 to 360): "+angle.ToString("0.00"));
	angle = GUILayout.HorizontalSlider (angle, 0, 360, GUILayout.Width (200));
	for (var i : int = 0; i<=360; i+=45) {
		if (Mathf.Abs (angle - i) < 10)
			angle = i;
	}
	
	GUILayout.Label ("Walking (0 to 1): "+walking.ToString("0.00"));
	walking = GUILayout.HorizontalSlider (walking, 0, 1, GUILayout.Width (100));
}
