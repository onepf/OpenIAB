
// ShaderDatabase
// knows and eventually "cooks" shaders in the beginning of the game (see CookShaders),
// also knows some tricks to hide the frame buffer with white and/or black planes
// to hide loading artefacts or shader cooking process

#pragma strict 

@script RequireComponent(Camera)

public var shaders : Shader[];
public var cookShadersOnMobiles : boolean = true;
public var cookShadersCover : Material;
private var cookShadersObject : GameObject;

function Awake () {	
#if UNITY_IPHONE || UNITY_ANDROID
	Screen.sleepTimeout = 0.0f;

	if (!cookShadersOnMobiles)
		return;
		
	if (!cookShadersCover.HasProperty ("_TintColor"))
		Debug.LogWarning ("Dualstick: the CookShadersCover material needs a _TintColor property to properly hide the cooking process", transform);
	
	CreateCameraCoverPlane ();
	cookShadersCover.SetColor ("_TintColor", Color (0.0,0.0,0.0,1.0));
#endif
}

function CreateCameraCoverPlane () : GameObject {
	cookShadersObject = GameObject.CreatePrimitive (PrimitiveType.Cube);
	cookShadersObject.renderer.material = cookShadersCover;	
	cookShadersObject.transform.parent = transform;
	cookShadersObject.transform.localPosition = Vector3.zero;
	cookShadersObject.transform.localPosition.z += 1.55;
	cookShadersObject.transform.localRotation = Quaternion.identity;
	cookShadersObject.transform.localEulerAngles.z += 180;
	cookShadersObject.transform.localScale = Vector3.one *1.5;	
	cookShadersObject.transform.localScale.x *= 1.6;	
	
	return cookShadersObject;		
}

function WhiteOut () {
	CreateCameraCoverPlane ();
	var mat : Material  = cookShadersObject.renderer.sharedMaterial;
	mat.SetColor ("_TintColor", Color (1.0f, 1.0f, 1.0f, 0.0));	
	
	yield;
	
	var c : Color = Color (1.0f, 1.0f, 1.0f, 0.0);
	while (c.a < 1.0) {
		c.a += Time.deltaTime * 0.25;
		mat.SetColor ("_TintColor", c);
		yield;
	}
			
	DestroyCameraCoverPlane ();
}

function WhiteIn () {	
	CreateCameraCoverPlane ();
	var mat : Material  = cookShadersObject.renderer.sharedMaterial;
	mat.SetColor ("_TintColor", Color (1.0f, 1.0f, 1.0f, 1.0));	
	
	yield;
	
	var c : Color = Color (1.0f, 1.0f, 1.0f, 1.0);
	while (c.a > 0.0) {
		c.a -= Time.deltaTime * 0.25;
		mat.SetColor ("_TintColor", c);
		yield;
	}
			
	DestroyCameraCoverPlane ();
}

function DestroyCameraCoverPlane () {
	if (cookShadersObject)
		DestroyImmediate (cookShadersObject);	
	cookShadersObject = null;
}

function Start () {	
#if UNITY_IPHONE || UNITY_ANDROID	
	if (cookShadersOnMobiles)
		yield CookShaders ();	
#endif
}

// this function is cooking all shaders to be used in the game. 
// it's good practice to draw all of them in order to avoid
// triggering in game shader compilations which might cause evil
// frame time spikes

// currently only enabled for mobile (iOS and Android) platforms

function CookShaders () {
	if (shaders.length) {
		var m : Material = new Material (shaders[0]);
		var cube : GameObject = GameObject.CreatePrimitive (PrimitiveType.Cube);
	
		cube.transform.parent = transform;
		cube.transform.localPosition = Vector3.zero;
		cube.transform.localPosition.z += 4.0;
			
		yield;	
		
		for (var s : Shader in shaders) {
			if (s) {
				m.shader = s;
				cube.renderer.material = m;
			}
			yield;
		}
					 
		Destroy (m);
		Destroy (cube);
		
		yield;
		var c : Color = Color.black;
		c.a = 1.0;
		while (c.a>0.0) {
			c.a -= Time.deltaTime*0.5;
			cookShadersCover.SetColor ("_TintColor", c);
			yield;
		}
	}

	DestroyCameraCoverPlane ();
}
