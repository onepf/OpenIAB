#pragma strict

// TODO: Wait for this - will work in 3.3
//import System.Collections.Generic;

@script RequireComponent (Collider)

public var pingPong : boolean = false;
// TODO: In Unity 3.3 remove the System.Collections.Generic and impoprt the namespace instead
public var patrolPoints : System.Collections.Generic.List.<PatrolPoint> = new System.Collections.Generic.List.<PatrolPoint> ();

private var activePatrollers : System.Collections.Generic.List.<GameObject> = new System.Collections.Generic.List.<GameObject> ();

function Register (go : GameObject) {
	activePatrollers.Add (go);
}

function UnRegister (go : GameObject) {
	activePatrollers.Remove (go);
}

function OnTriggerEnter (other : Collider) {
	if (activePatrollers.Contains (other.gameObject)) {
		var ai : AI = other.gameObject.GetComponentInChildren.<AI> ();
		if (ai)
			ai.OnEnterInterestArea ();
	}
}

function OnTriggerExit (other : Collider) {
	if (activePatrollers.Contains (other.gameObject)) {
		var ai : AI = other.gameObject.GetComponentInChildren.<AI> ();
		if (ai)
			ai.OnExitInterestArea ();
	}
}

function GetClosestPatrolPoint (pos : Vector3) : int {
	if (patrolPoints.Count == 0)
		return 0;
	
	var shortestDist : float = Mathf.Infinity;
	var shortestIndex : int = 0;
	for (var i : int = 0; i < patrolPoints.Count; i++) {
		patrolPoints[i].position = patrolPoints[i].transform.position;
		var dist : float = (patrolPoints[i].position - pos).sqrMagnitude;
		if (dist < shortestDist) {
			shortestDist = dist;
			shortestIndex = i;
		}
	}
	
	// If going towards the closest point makes us go in the wrong direction,
	// choose the next point instead.
	if (!pingPong || shortestIndex < patrolPoints.Count - 1) {
		var nextIndex : int = (shortestIndex + 1) % patrolPoints.Count;
		var angle : float = Vector3.Angle (
			patrolPoints[nextIndex].position - patrolPoints[shortestIndex].position,
			patrolPoints[shortestIndex].position - pos
		);
		if (angle > 120)
			shortestIndex = nextIndex;
	}
	
	return shortestIndex;
}

function OnDrawGizmos () {
	if (patrolPoints.Count == 0)
		return;
	
	Gizmos.color = Color (0.5, 0.5, 1.0);
	
	var lastPoint : Vector3 = patrolPoints[0].transform.position;
	var loopCount = patrolPoints.Count;
	if (pingPong)
		loopCount--;
	for (var i : int = 0; i < loopCount; i++) {
		if (!patrolPoints[i])
			break;
		var newPoint = patrolPoints[(i + 1) % patrolPoints.Count].transform.position;
		Gizmos.DrawLine (lastPoint, newPoint);
		lastPoint = newPoint;
	}
}

function GetIndexOfPatrolPoint (point : PatrolPoint) {
	for (var i : int = 0; i < patrolPoints.Count; i++) {
		if (patrolPoints[i] == point)
			return i;
	}
	return -1;
}

function InsertPatrolPointAt (index : int) : GameObject {
	var go = new GameObject ("PatrolPoint", PatrolPoint);
	go.transform.parent = transform;
	var count : int = patrolPoints.Count;
	
	if (count == 0) {
		go.transform.localPosition = Vector3.zero;
		patrolPoints.Add(go.GetComponent.<PatrolPoint>());
	}
	else {
		if (!pingPong || (index > 0 && index < count) || count < 2) {
			index = index % count;
			var prevIndex : int = index - 1;
			if (prevIndex < 0)
				prevIndex += count;
			
			go.transform.position = (
				patrolPoints[prevIndex].transform.position
				+ patrolPoints[index].transform.position
			) * 0.5;
		}
		else if (index == 0) {
			go.transform.position = (
				patrolPoints[0].transform.position * 2
				- patrolPoints[1].transform.position
			);
		}
		else {
			go.transform.position = (
				patrolPoints[count-1].transform.position * 2
				- patrolPoints[count-2].transform.position
			);
		}
		patrolPoints.Insert(index, go.GetComponent.<PatrolPoint>());
	}
	
	return go;
}

function RemovePatrolPointAt (index : int) {
	var go : GameObject = patrolPoints[index].gameObject;
	patrolPoints.RemoveAt (index);
	DestroyImmediate (go);
}
