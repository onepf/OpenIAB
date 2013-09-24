#pragma strict
#pragma downcast

//import UnityEditor;

// MenuItem adds a menu item in the GameObject menu
// and executes the following function when clicked
@MenuItem ("Tools/Sample Animation On Selected")
static function SampleAnimation () {
	var anim : Animation = Selection.activeGameObject.animation;
	if (anim != null) {
		Selection.activeGameObject.SampleAnimation(anim.clip, 0);
	}
}
