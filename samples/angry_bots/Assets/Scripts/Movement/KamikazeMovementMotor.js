#pragma strict

class KamikazeMovementMotor extends MovementMotor {
	
	public var flyingSpeed : float = 5.0;
	public var zigZagness : float = 3.0f;
	public var zigZagSpeed : float = 2.5f;
	public var oriantationMultiplier : float = 2.5f;
	public var backtrackIntensity : float = 0.5f;
	
	private var smoothedDirection : Vector3 = Vector3.zero;;
			
	function FixedUpdate () {
		var dir : Vector3 = movementTarget - transform.position;
		var zigzag : Vector3 = transform.right * (Mathf.PingPong (Time.time * zigZagSpeed, 2.0) - 1.0) * zigZagness;

		dir.Normalize ();
		
		smoothedDirection = Vector3.Slerp (smoothedDirection, dir, Time.deltaTime * 3.0f);
		var orientationSpeed = 1.0f;
				
		var deltaVelocity : Vector3 = (smoothedDirection * flyingSpeed + zigzag) - rigidbody.velocity;		
		if (Vector3.Dot (dir, transform.forward) > 0.8f)
			rigidbody.AddForce (deltaVelocity, ForceMode.Force);
		else {
			rigidbody.AddForce (-deltaVelocity * backtrackIntensity, ForceMode.Force);	
			orientationSpeed = oriantationMultiplier;
		}
		
		// Make the character rotate towards the target rotation
		var faceDir : Vector3 = smoothedDirection;
		if (faceDir == Vector3.zero) {
			rigidbody.angularVelocity = Vector3.zero;
		}
		else {
			var rotationAngle : float = AngleAroundAxis (transform.forward, faceDir, Vector3.up);
			rigidbody.angularVelocity = (Vector3.up * rotationAngle * 0.2f * orientationSpeed);
		}		
	
	}
	
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
	
	function OnCollisionEnter (collisionInfo : Collision) {
	}
	
}
