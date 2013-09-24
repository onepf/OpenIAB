
// about the MoodBox (tm) system
//
// MoodBoxManager, MoodBox and MoodBoxData and friends are being used to enable
// local bloom, noise, color correction, etc.
//
// The player triggers MoodBoxes by walking through them, so they are placed strategically
// at entrances and exits. custom effect values will then be interpolated each frame and 
// given to (image) effects and shaders.

#pragma strict

@script ExecuteInEditMode ();

public static var current : MoodBox = null;
public var currentData : MoodBoxData;

public var bloom : MobileBloom;
public var noise : ColoredNoise;
public var fog : RenderFogPlane;

public var startMoodBox : MoodBox;
public var defaultPlayerReflection : Cubemap;
public var playerReflectionMaterials : Material[];
public var applyNearestMoodBox : boolean = false;

public var currentMoodBox : MoodBox = null;

@HideInInspector
public var splashManagers : GameObject[];
@HideInInspector
public var rainManagers : GameObject[];

public function Awake () {
	// mood boxes have the power to disable expensive effects when it makes sense
	
	splashManagers = GameObject.FindGameObjectsWithTag ("RainSplashManager");
	rainManagers = GameObject.FindGameObjectsWithTag ("RainBoxManager");
}

public function Start () {
	if (!bloom)
		bloom = Camera.main.gameObject.GetComponent.<MobileBloom> ();	
	if (!noise)
		noise = Camera.main.gameObject.GetComponent.<ColoredNoise> ();	
	if (!fog)
		fog = Camera.main.gameObject.GetComponentInChildren.<RenderFogPlane> ();
		
	current = startMoodBox;	
	UpdateFromMoodBox ();
}

public function Update () {
	UpdateFromMoodBox ();
}

public function GetData () {
	return currentData;
}

function UpdateFromMoodBox () {

#if UNITY_EDITOR	
	ApplyNearestMoodBoxIfDesired ();
#endif

	// we want to see what the current mood box is in the editor
	currentMoodBox = current;

	if (current) {	
		if (!Application.isPlaying) {	
			currentData.noiseAmount = current.data.noiseAmount;
			currentData.colorMixBlend = current.data.colorMixBlend;
			currentData.colorMix = current.data.colorMix;
			currentData.fogY = current.data.fogY;
			currentData.fogColor = current.data.fogColor;
			currentData.outside = current.data.outside;		
		} 
		else { 
			// play mode, interpolate nicely
			currentData.noiseAmount = Mathf.Lerp (currentData.noiseAmount, current.data.noiseAmount, Time.deltaTime);		
			currentData.colorMixBlend = Mathf.Lerp (currentData.colorMixBlend, current.data.colorMixBlend, Time.deltaTime);
			currentData.colorMix = Color.Lerp (currentData.colorMix, current.data.colorMix, Time.deltaTime);
			currentData.fogY = Mathf.Lerp (currentData.fogY, current.data.fogY, Time.deltaTime * 1.5f);	
			currentData.fogColor = Color.Lerp (currentData.fogColor, current.data.fogColor, Time.deltaTime * 0.25f);
			currentData.outside = current.data.outside;			
		}	
	} 	

	// apply new mood and effect values to actual effects (if in use)
			
	if (bloom && bloom.enabled) {
		bloom.colorMix = currentData.colorMix;
		bloom.colorMixBlend = currentData.colorMixBlend;		
	}
	if (noise && noise.enabled) {
		noise.localNoiseAmount = currentData.noiseAmount;
	}
	if (fog && fog.enabled) {
		fog.renderer.sharedMaterial.SetFloat ("_Y", currentData.fogY);
		fog.renderer.sharedMaterial.SetColor ("_FogColor", currentData.fogColor);			
	}
}

function ApplyNearestMoodBoxIfDesired () {
	if (applyNearestMoodBox) { 
		var boxes : Component[] = null;
		boxes = GetComponentsInChildren (MoodBox); // as MoodBox[];
		if (boxes) {
			var cameraPos : Vector3 = Camera.main.transform.position;
			var minMoodBox = boxes[0] as MoodBox;
			var minDistance = Mathf.Infinity;
			for (var b : Component in boxes) {
				if ( ((b as MoodBox).transform.position - cameraPos).sqrMagnitude < minDistance) {
					minDistance = ((b as MoodBox).transform.position - cameraPos).sqrMagnitude;
					minMoodBox = b as MoodBox;
				}
			}
			current = minMoodBox;
		}
		else
			Debug.Log ("no MoodBox components found ...");
		
		applyNearestMoodBox = false;	
	}	
}