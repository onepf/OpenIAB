#pragma strict
#pragma downcast

//import UnityEditor;

// MenuItem adds a menu item in the GameObject menu
// and executes the following function when clicked
@MenuItem ("Tools/Use Only Unlit Shaders")
static function SampleAnimation () {
	var renderers : Renderer[] = FindObjectsOfType (Renderer);
	for (var renderer : Renderer in renderers) {
		renderer.sharedMaterial.shader = Shader.Find( "Unlit/Texture" );
	}
}
