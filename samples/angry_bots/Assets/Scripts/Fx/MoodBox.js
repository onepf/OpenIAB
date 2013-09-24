
#pragma strict

@script RequireComponent (BoxCollider)

public var data : MoodBoxData;
public var playerReflection : Cubemap;

private var manager : MoodBoxManager;

function Start () {
	manager = transform.parent.GetComponent (MoodBoxManager) as MoodBoxManager;
	if (!manager) {
		Debug.Log ("Disabled moodbox " + gameObject.name + " as a MoodBoxManager was not found.", transform);	
		enabled = false;
	}
}

function OnDrawGizmos () {
	if (transform.parent) {
		Gizmos.color = Color (0.5f, 0.9f, 1.0f, 0.15f);
		Gizmos.DrawCube (collider.bounds.center, collider.bounds.size );
	}
}

function OnDrawGizmosSelected () {
	if (transform.parent) {
		Gizmos.color = Color (0.5f, 0.9f, 1.0f, 0.75f);
		Gizmos.DrawCube (collider.bounds.center, collider.bounds.size );
	}
}

function OnTriggerEnter (other : Collider) {
	if (other.tag == "Player")
		ApplyMoodBox ();
}

function ApplyMoodBox () {	
	
	// optimization: deactivate rain stuff a little earlier
	
	if (manager.GetData ().outside != data.outside) {
		for (var m : GameObject in manager.rainManagers) {
			m.SetActiveRecursively (data.outside);	
		}
		for (var m : GameObject in manager.splashManagers) {
			m.SetActiveRecursively (data.outside);	
		}		
	}
	
	manager.current = this;	
	
	if (manager.playerReflectionMaterials.Length) {
		for (var m : Material in manager.playerReflectionMaterials)
			m.SetTexture ("_Cube", playerReflection ? playerReflection : manager.defaultPlayerReflection);
	}	
}