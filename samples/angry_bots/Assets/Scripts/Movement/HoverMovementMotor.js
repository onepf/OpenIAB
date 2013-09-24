#pragma strict

@script RequireComponent (Rigidbody)

class HoverMovementMotor extends MovementMotor {
	
	//public var movement : MoveController;
	public var flyingSpeed : float = 5.0;
	public var flyingSnappyness : float = 2.0;
	public var turningSpeed : float = 3.0;
	public var turningSnappyness : float = 3.0;
	public var bankingAmount : float = 1.0;
	
	function FixedUpdate () {
		// Handle the movement of the character
		var targetVelocity : Vector3 = movementDirection * flyingSpeed;
		var deltaVelocity : Vector3 = targetVelocity - rigidbody.velocity;
		rigidbody.AddForce (deltaVelocity * flyingSnappyness, ForceMode.Acceleration);
		
		// Make the character rotate towards the target rotation
		var facingDir : Vector3 = facingDirection != Vector3.zero ? facingDirection : movementDirection;
		if (facingDir != Vector3.zero) {
			var targetRotation = Quaternion.LookRotation (facingDir, Vector3.up);
			var deltaRotation = targetRotation * Quaternion.Inverse(transform.rotation);
			var axis : Vector3;
			var angle : float;
			deltaRotation.ToAngleAxis (angle, axis);
			var deltaAngularVelocity : Vector3 = axis * Mathf.Clamp (angle, -turningSpeed, turningSpeed) - rigidbody.angularVelocity;
			
			var banking : float = Vector3.Dot (movementDirection, -transform.right);
			
			rigidbody.AddTorque (deltaAngularVelocity * turningSnappyness + transform.forward * banking * bankingAmount);
		}
	}
	
	function OnCollisionStay (collisionInfo : Collision) {
		// Move up if colliding with static geometry
		if (collisionInfo.rigidbody == null)
			rigidbody.velocity += Vector3.up * Time.deltaTime * 50;
	}
	
}
