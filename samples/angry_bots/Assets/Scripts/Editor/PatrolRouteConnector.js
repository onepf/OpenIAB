#pragma strict
#pragma downcast

//import UnityEditor;

// MenuItem adds a menu item in the GameObject menu
// and executes the following function when clicked
@MenuItem ("Tools/Assign Closest Patrol Routes")
static function AssignPatrolRoutes () {
	var points : PatrolPoint[] = FindObjectsOfType (PatrolPoint);
	var patrollers : PatrolMoveController[] = FindObjectsOfType (PatrolMoveController);
	var connected : int = 0;
	
	for (var patroller : PatrolMoveController in patrollers) {
		var closestDist : float = Mathf.Infinity;
		var closestPoint : PatrolPoint;
		for (var point : PatrolPoint in points) {
			var dist : float = (patroller.transform.position - point.transform.position).magnitude;
			if (dist < closestDist) {
				closestPoint = point;
				closestDist = dist;
			}
		}
		if (closestDist != null) {
			patroller.patrolRoute = closestPoint.transform.parent.GetComponent.<PatrolRoute>();
			connected++;
		}
	}
	
	Debug.Log("Successfully connected routes to "+connected+" out of "+patrollers.Length+" patrollers.");
}
