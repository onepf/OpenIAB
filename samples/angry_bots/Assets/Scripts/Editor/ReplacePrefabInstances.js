#pragma strict

class ReplacePrefabInstances extends ScriptableWizard {
	
	var originalPrefab : GameObject;
	var replacementPrefab : GameObject;
	
	@MenuItem ("Tools/Replace Prefab Instances")
	static function CreateWizard () {
		ScriptableWizard.DisplayWizard.<ReplacePrefabInstances> ("Replace Prefab Instances", "Replace");
	}
	function OnWizardCreate () {
		if (!originalPrefab || !replacementPrefab)
			return;
		
		var gos : UnityEngine.Object[] = FindObjectsOfType (GameObject);
		for (var i : int = 0; i<gos.Length; i++) {
			if (EditorUtility.GetPrefabParent (gos[i]) == originalPrefab) {
				var oldGo : GameObject = gos[i] as GameObject;
				var newGo : GameObject = EditorUtility.InstantiatePrefab (replacementPrefab) as GameObject;
				newGo.transform.parent = oldGo.transform.parent;
				newGo.transform.localPosition = oldGo.transform.localPosition;
				newGo.transform.localRotation = oldGo.transform.localRotation;
				newGo.transform.localScale = oldGo.transform.localScale;
				newGo.isStatic = oldGo.isStatic;
				newGo.layer = oldGo.layer;
				newGo.tag = oldGo.tag;
				newGo.name = oldGo.name.Replace (originalPrefab.name, replacementPrefab.name);
				DestroyImmediate (oldGo);
			}
		}
	}
	
}
