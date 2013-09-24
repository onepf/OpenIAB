#pragma strict

@script RequireComponent (Health)

private var health : Health;
private var animationComp : Animation;

health = GetComponent.<Health> ();
animationComp = GetComponentInChildren.<Animation> ();

function Start () {
	UpdateHackingProgress ();
	enabled = false;
}

function OnTriggerStay (other : Collider) {
	if (other.gameObject.tag == "Player")
		health.OnDamage (Time.deltaTime, Vector3.zero);
}

function OnHacking () {
	enabled = true;
	UpdateHackingProgress ();
}

function OnHackingCompleted () {
	audio.Play ();
	animationComp.Stop ();
	enabled = false;
}

function UpdateHackingProgress () {
	animationComp.gameObject.SampleAnimation (animationComp.clip, (1 - health.health / health.maxHealth) * animationComp.clip.length);
}

function Update () {;
	UpdateHackingProgress ();
	
	if (health.health == 0 || health.health == health.maxHealth) {
		UpdateHackingProgress ();
		enabled = false;
	}
}