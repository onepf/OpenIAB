#pragma strict

// Public member data
public var motor : MovementMotor;

public var patrolRoute : PatrolRoute;
public var patrolPointRadius : float = 0.5;

// Private memeber data
private var character : Transform;
private var nextPatrolPoint : int = 0;
private var patrolDirection : int = 1;

function Start () {
	character = motor.transform;
	patrolRoute.Register (transform.parent.gameObject);
}

function OnEnable () {
	nextPatrolPoint = patrolRoute.GetClosestPatrolPoint (transform.position);
}

function OnDestroy () {
	patrolRoute.UnRegister (transform.parent.gameObject);
}

function Update () {
	// Early out if there are no patrol points
	if (patrolRoute == null || patrolRoute.patrolPoints.Count == 0)
		return;
	
	// Find the vector towards the next patrol point.
	var targetVector : Vector3 = patrolRoute.patrolPoints[nextPatrolPoint].position - character.position;
	targetVector.y = 0;
	
	// If the patrol point has been reached, select the next one.
	if (targetVector.sqrMagnitude < patrolPointRadius * patrolPointRadius) {
		nextPatrolPoint += patrolDirection;
		if (nextPatrolPoint < 0) {
			nextPatrolPoint = 1;
			patrolDirection = 1;
		}
		if (nextPatrolPoint >= patrolRoute.patrolPoints.Count) {
			if (patrolRoute.pingPong) {
				patrolDirection = -1;
				nextPatrolPoint = patrolRoute.patrolPoints.Count - 2;
			}
			else {
				nextPatrolPoint = 0;
			}
		}
	}
	
	// Make sure the target vector doesn't exceed a length if one
	if (targetVector.sqrMagnitude > 1)
		targetVector.Normalize ();
	
	// Set the movement direction.
	motor.movementDirection = targetVector;
	// Set the facing direction.
	motor.facingDirection = targetVector;
}
