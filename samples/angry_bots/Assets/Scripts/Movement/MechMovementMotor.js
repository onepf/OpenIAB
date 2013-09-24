#pragma strict

@script RequireComponent (Rigidbody)

class MechMovementMotor extends MovementMotor {
	
	public var walkingSpeed : float = 3.0;
	public var turningSpeed : float = 100.0;
	public var aimingSpeed : float = 150.0;
	
	public var head : Transform;
	
	//private var wallNormal : Vector3 = Vector3.zero;
	private var wallHit : Vector3;
	private var facingInRightDirection : boolean = false;
	private var headRotation : Quaternion = Quaternion.identity;
	
	function FixedUpdate () {
		var adjustedMovementDirection : Vector3 = movementDirection;
		
		// If the movement direction points into a wall as defined by the wall normal,
		// then change the movement direction to be perpendicular to the wall,
		// so the character "glides" along the wall.
		/*if (Vector3.Dot (movementDirection, wallNormal) < 0) {
			// Keep the vector length prior to adjustment
			var vectorLength : float = movementDirection.magnitude;
			// Project the movement vector onto the plane defined by the wall normal
			adjustedMovementDirection =
				movementDirection - Vector3.Project (movementDirection, wallNormal) * 0.9;
			// Apply the original length of the vector
			adjustedMovementDirection = adjustedMovementDirection.normalized * vectorLength;
		}*/
		
		/*Debug.DrawRay(transform.position, adjustedMovementDirection, Color.yellow);
		Debug.DrawRay(transform.position, movementDirection, Color.green);
		Debug.DrawRay(transform.position, wallNormal, Color.red);*/
		
		// Make the character rotate towards the target rotation
		var rotationAngle : float;
		if (adjustedMovementDirection != Vector3.zero)
			rotationAngle = AngleAroundAxis (transform.forward, adjustedMovementDirection, Vector3.up) * 0.3;
		else
			rotationAngle = 0;
		var targetAngularVelocity : Vector3 = Vector3.up * Mathf.Clamp (rotationAngle, -turningSpeed * Mathf.Deg2Rad, turningSpeed * Mathf.Deg2Rad);
		rigidbody.angularVelocity = Vector3.MoveTowards (rigidbody.angularVelocity, targetAngularVelocity, Time.deltaTime * turningSpeed * Mathf.Deg2Rad * 3);
		
		/*
		if ((transform.position - wallHit).magnitude > 2) {
			wallNormal = Vector3.zero;
		}*/
		
		var angle : float = Vector3.Angle (transform.forward, adjustedMovementDirection);
		if (facingInRightDirection && angle > 25)
			facingInRightDirection = false;
		if (!facingInRightDirection && angle < 5)
			facingInRightDirection = true;
		
		// Handle the movement of the character
		var targetVelocity : Vector3;
		if (facingInRightDirection)
			targetVelocity = transform.forward * walkingSpeed + rigidbody.velocity.y * Vector3.up;
		else
			targetVelocity = rigidbody.velocity.y * Vector3.up;
		
		rigidbody.velocity = Vector3.MoveTowards (rigidbody.velocity, targetVelocity, Time.deltaTime * walkingSpeed * 3);
		//transform.position += targetVelocity * Time.deltaTime * walkingSpeed * 3;
	}
	
	function LateUpdate () {
		// Target with head
		if (facingDirection != Vector3.zero) {
			var targetRotation : Quaternion = Quaternion.LookRotation (facingDirection);
			headRotation = Quaternion.RotateTowards (
				headRotation,
				targetRotation,
				aimingSpeed * Time.deltaTime
			);
			head.rotation = headRotation * Quaternion.Inverse (transform.rotation) * head.rotation;
		}
	}
	
	/*
	function OnCollisionStay (collisionInfo : Collision) {
		if (collisionInfo.gameObject.tag == "Player")
			return;
		
		// Record the first wall normal
		for (var contact : ContactPoint in collisionInfo.contacts) {
			// Discard normals that are not mostly horizontal
			if (Mathf.Abs(contact.normal.y) < 0.7) {
				wallNormal = contact.normal;
				wallNormal.y = 0;
				wallHit = transform.position;
				break;
			}
		}
		
		// Only keep the horizontal components
		wallNormal.y = 0;
	}
	*/
	
	// The angle between dirA and dirB around axis
	static function AngleAroundAxis (dirA : Vector3, dirB : Vector3, axis : Vector3) {
	    // Project A and B onto the plane orthogonal target axis
	    dirA = dirA - Vector3.Project (dirA, axis);
	    dirB = dirB - Vector3.Project (dirB, axis);
	   
	    // Find (positive) angle between A and B
	    var angle : float = Vector3.Angle (dirA, dirB);
	   
	    // Return angle multiplied with 1 or -1
	    return angle * (Vector3.Dot (axis, Vector3.Cross (dirA, dirB)) < 0 ? -1 : 1);
	}
	
}
