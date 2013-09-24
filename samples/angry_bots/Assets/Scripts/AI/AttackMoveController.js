#pragma strict

// Public member data
public var motor : MovementMotor;

public var targetDistanceFront : float = 2.5;
public var targetDistanceBack : float = 1.0;

public var weaponBehaviour : MonoBehaviour;

// Private memeber data
private var character : Transform;
private var nextPatrolPoint : int = 0;

private var player : Transform;

private var goingRight : float = 1;
private var directionChangeTime : float = 0;

function Awake () {
	character = motor.transform;
	player = GameObject.FindWithTag ("Player").transform;
}

function OnEnable () {
	if (weaponBehaviour)
		weaponBehaviour.enabled = true;
}

function OnDisable () {
	if (weaponBehaviour)
		weaponBehaviour.enabled = false;
}

function Update () {
	// Calculate the direction from the player to this character
	var playerToCharacterDirection : Vector3 = (character.position - player.position);
	//playerToCharacterDirection.y = 0;
	playerToCharacterDirection.Normalize ();
	
	// Set this character to face the player,
	// that is, to face the direction from this character to the player
	motor.facingDirection = -playerToCharacterDirection;
	
	// Calculate the angle in degrees this character is away from the front of the character.
	// If this character is in front of the player, the angle is 0,
	// to the side is 90 and behind is 180.
	var degreesFromPlayerForward : float = Vector3.Angle (playerToCharacterDirection, player.forward);
	
	// If not almost directly in front of the player, base the direction on what
	// is the shortest route to get behind the player: Left or right way around.
	if (degreesFromPlayerForward > 30) {
		goingRight = Mathf.Sign (Vector3.Dot (-playerToCharacterDirection, player.right));
		directionChangeTime = Time.time;
	}
	// If this character has stayed in front of the player for more than a little while,
	// change direction to the other way around. This avoids some cases of the character
	// getting stuck while the player is aiming at him.
	else if (Time.time > directionChangeTime + 0.8) {
		goingRight = -goingRight;
		directionChangeTime = Time.time;
	}
	
	// behindFactor is how much this character is behind the player.
	// 0 degrees is a value of 0 and 90 degrees or more is a value of 1.
	var behindFactor : float = Mathf.InverseLerp (0, 90, degreesFromPlayerForward);
	
	// Lerp the target distance between the front and back target distances based on the behindFactor
	var targetDistance : float = Mathf.Lerp (targetDistanceFront, targetDistanceBack, behindFactor);
	
	// Calculate the targetPosition
	var targetPosition : Vector3 = player.position + playerToCharacterDirection * targetDistance;
	
	// If this character is not already behind the player, strafe sideways to get behind
	if (degreesFromPlayerForward < 120)
		targetPosition += character.right * goingRight;
	
	// Calculate the target vector that this character should move along
	// to reach the target position
	var targetVector : Vector3 = targetPosition - character.position;
	targetVector.y = 0;
	
	// Make sure the target vector doesn't exceed a length if one
	if (targetVector.sqrMagnitude > 1)
		targetVector.Normalize ();
	
	// Smooth the movement direction a bit so this character doesn't change direction abruptly
	motor.movementDirection = targetVector;
}
