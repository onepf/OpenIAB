
#pragma strict

@script RequireComponent (BoxCollider)

public var timeToTriggerLevelEnd : float = 2.0f;
public var endSceneName : String = "3-4_Pain";


function OnTriggerEnter (other : Collider) {
	if (other.tag == "Player") {
		
		FadeOutAudio ();	
		
		var playerMove : PlayerMoveController = other.gameObject.GetComponent.<PlayerMoveController> ();
		playerMove.enabled = false;
		
		yield;
		
		var timeWaited : float = 0.0f;
		var playerMotor : FreeMovementMotor = other.gameObject.GetComponent.<FreeMovementMotor> ();
		while (playerMotor.walkingSpeed > 0.0f) {
			playerMotor.walkingSpeed -= Time.deltaTime * 6.0f;
			if (playerMotor.walkingSpeed < 0.0f)
				playerMotor.walkingSpeed = 0.0f;
			timeWaited += Time.deltaTime;
			yield;
		}
		playerMotor.walkingSpeed = 0.0f;		
		
		yield WaitForSeconds ( Mathf.Clamp (timeToTriggerLevelEnd - timeWaited, 0.0f, timeToTriggerLevelEnd));
		Camera.main.gameObject.SendMessage ("WhiteOut");
		
		yield WaitForSeconds (2.0);
		
		Application.LoadLevel (endSceneName);
	}
}

function FadeOutAudio () {
	var al : AudioListener = Camera.main.gameObject.GetComponent.<AudioListener> ();
	if (al) {
		while (al.volume > 0.0f) {
			al.volume -= Time.deltaTime / timeToTriggerLevelEnd;
			yield;	
		}	
	}		
}