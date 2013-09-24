
#pragma strict

@script RequireComponent (Camera)

function Start () {
	var al : AudioListener = null;
	al = Camera.main.gameObject.GetComponent.<AudioListener> ();
	
	if (al)
		al.volume = 1.0f;
		
	var sm : ShaderDatabase = GetComponent.<ShaderDatabase> ();
	sm.WhiteIn ();
	
	camera.backgroundColor = Color.white;
}