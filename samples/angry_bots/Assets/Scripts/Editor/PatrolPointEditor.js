#pragma strict

@CustomEditor(PatrolPoint)
class PatrolPointEditor extends Editor {
	function OnInspectorGUI () {
		var point : PatrolPoint = target as PatrolPoint;
		var route : PatrolRoute = point.transform.parent.GetComponent.<PatrolRoute>();
		var thisIndex : int = route.GetIndexOfPatrolPoint (point);
		
		if (GUILayout.Button ("Remove This Patrol Point")) {
			route.RemovePatrolPointAt (thisIndex);
			var newSelectionIndex : int = Mathf.Clamp (thisIndex, 0, route.patrolPoints.Count - 1);
			Selection.activeGameObject = route.patrolPoints[newSelectionIndex].gameObject;
		}
		if (GUILayout.Button ("Insert Patrol Point Before")) {
			Selection.activeGameObject = route.InsertPatrolPointAt (thisIndex);
		}
		if (GUILayout.Button ("Insert Patrol Point After")) {
			Selection.activeGameObject = route.InsertPatrolPointAt (thisIndex + 1);
		}
	}
	
	function OnSceneGUI () {
		var point : PatrolPoint = target as PatrolPoint;
		var route : PatrolRoute = point.transform.parent.GetComponent.<PatrolRoute>();
		
		PatrolRouteEditor.DrawPatrolRoute (route);
	}
}
