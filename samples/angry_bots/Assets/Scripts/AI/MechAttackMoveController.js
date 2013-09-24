#pragma strict

// Public member data
public var motor : MovementMotor;
public var head : Transform;

public var targetDistanceMin : float = 3.0;
public var targetDistanceMax : float = 4.0;

public var weaponBehaviours : MonoBehaviour[];
public var fireFrequency : float = 2;

// Private memeber data
private var ai : AI;

private var character : Transform;

private var player : Transform;

private var inRange : boolean = false;
private var nextRaycastTime : float = 0;
private var lastRaycastSuccessfulTime : float = 0;
private var noticeTime : float = 0;

private var firing : boolean = false;
private var lastFireTime : float = -1;
private var nextWeaponToFire : int = 0;

function Awake () {
	character = motor.transform;
	player = GameObject.FindWithTag ("Player").transform;
	ai = transform.parent.GetComponentInChildren.<AI> ();
}

function OnEnable () {
	inRange = false;
	nextRaycastTime = Time.time + 1;
	lastRaycastSuccessfulTime = Time.time;
	noticeTime = Time.time;
}

function OnDisable () {
	Shoot (false);
}

function Shoot (state : boolean) {
	firing = state;
}

function Fire () {
	if (weaponBehaviours[nextWeaponToFire]) {
		weaponBehaviours[nextWeaponToFire].SendMessage ("Fire");
		nextWeaponToFire = (nextWeaponToFire + 1) % weaponBehaviours.Length;
		lastFireTime = Time.time;
	}
}

function Update () {
	// Calculate the direction from the player to this character
	var playerDirection : Vector3 = (player.position - character.position);
	playerDirection.y = 0;
	var playerDist : float = playerDirection.magnitude;
	playerDirection /= playerDist;
	
	// Set this character to face the player,
	// that is, to face the direction from this character to the player
	motor.facingDirection = playerDirection;
	
	// For a short moment after noticing player,
	// only look at him but don't walk towards or attack yet.
	if (Time.time < noticeTime + 1.5) {
		motor.movementDirection = Vector3.zero;
		return;
	}
	
	if (inRange && playerDist > targetDistanceMax)
		inRange = false;
	if (!inRange && playerDist < targetDistanceMin)
		inRange = true;
	
	if (inRange)
		motor.movementDirection = Vector3.zero;
	else
		motor.movementDirection = playerDirection;
	
	if (Time.time > nextRaycastTime) {
		nextRaycastTime = Time.time + 1;
		if (ai.CanSeePlayer ()) {
			lastRaycastSuccessfulTime = Time.time;
			if (IsAimingAtPlayer ())
				Shoot (true);
			else
				Shoot (false);
		}
		else {
			Shoot (false);
			if (Time.time > lastRaycastSuccessfulTime + 5) {
				ai.OnLostTrack ();
			}
		}
	}
	
	if (firing) {
		if (Time.time > lastFireTime + 1 / fireFrequency) {
			Fire ();
		}
	}
}

function IsAimingAtPlayer () : boolean {
	var playerDirection : Vector3 = (player.position - head.position);
	playerDirection.y = 0;
	return Vector3.Angle (head.forward, playerDirection) < 15;
}