#pragma strict

var bulletPrefab : GameObject;
var frequency : float = 2;
var coneAngle : float = 1.5;
var fireSound : AudioClip;
var firing : boolean = false;
var noisiness : float = 2.0f;

private var nextFireNoise : float = 1.0f;
private var lastFireTime : float = -1;


function Update () {
	if (firing) {
		if (Time.time > nextFireNoise + lastFireTime + 1 / frequency) {
			Fire ();
		}
	}
}

function Fire () {
	// Spawn visual bullet
	var coneRandomRotation = Quaternion.Euler (Random.Range (-coneAngle, coneAngle), Random.Range (-coneAngle, coneAngle), 0);
	Spawner.Spawn (bulletPrefab, transform.position, transform.rotation * coneRandomRotation);
	
	if (audio && fireSound) {
		audio.clip = fireSound;
		audio.Play ();
	}
	
	lastFireTime = Time.time;
	nextFireNoise = Random.value * noisiness;
}

function OnStartFire () {
	firing = true;
}

function OnStopFire () {
	firing = false;
}